"""
Export + quantize facebook/wav2vec2-lv-60-espeak-cv-ft for Android ONNX Runtime.

Yêu cầu:
  pip install "transformers>=4.40" "torch>=2.1" "onnx>=1.16" "onnxruntime>=1.18"
  pip install "huggingface_hub>=0.20"

  (KHÔNG cần `phonemizer` / `espeak` — script này chỉ tải file vocab.json
   từ HF Hub, không init Wav2Vec2PhonemeCTCTokenizer.)

Vì sao script này phức tạp hơn 1 lệnh export:

  (a) torch 2.5+ mặc định `torch.onnx.export(..., dynamo=True)`.
      Path dynamo CHIA model thành 1 file ONNX graph nhỏ (~3.5MB)
      + external data files chứa weights → quantize_dynamic không tìm
      thấy MatMul đúng cách, kết quả INT8 phình lên 357MB (= không
      quantize gì cả). Phải set `dynamo=False` để export thành 1 file
      duy nhất chứa cả weights inline.

  (b) Sau export, phải chạy `quant_pre_process` (shape inference +
      constant folding). Nếu không, MatMul ops bị wrap trong subgraph
      mà quantize_dynamic skip qua → quantize không có tác dụng.

  (c) Wav2Vec2PhonemeCTCTokenizer.from_pretrained() yêu cầu backend
      `phonemizer` (gọi espeak-ng nhị phân). Chỉ để init backend, mà ta
      lại không cần phonemize gì — chỉ cần file vocab.json. → Tải
      vocab.json + special_tokens_map.json trực tiếp qua huggingface_hub.

Output (copy cả 2 vào app/src/main/assets/):
  wav2vec2_phoneme.onnx        (~115MB INT8 sau quantize)
  wav2vec2_phoneme_vocab.json
"""

import glob
import json
import os

import torch
from transformers import Wav2Vec2ForCTC
from huggingface_hub import hf_hub_download
from onnxruntime.quantization import quantize_dynamic, QuantType
from onnxruntime.quantization.shape_inference import quant_pre_process

MODEL_ID   = "facebook/wav2vec2-lv-60-espeak-cv-ft"

FP32_PATH      = "wav2vec2_phoneme_fp32.onnx"
FP32_PREP_PATH = "wav2vec2_phoneme_fp32_prep.onnx"
INT8_PATH      = "wav2vec2_phoneme.onnx"
VOCAB_PATH     = "wav2vec2_phoneme_vocab.json"


def _cleanup(*paths):
    for p in paths:
        # bao gồm cả external data files nếu có (*.data, *.onnx_data, *.weight)
        for f in glob.glob(p) + glob.glob(p + ".*") + glob.glob(p + "_data*"):
            try:
                os.remove(f)
            except OSError:
                pass


# ─────────────────────────────────────────────
# 1) Export FP32 ONNX (dynamo=False → single file, weights inline)
# ─────────────────────────────────────────────
print(f"Loading {MODEL_ID} ...")
model = Wav2Vec2ForCTC.from_pretrained(MODEL_ID)
model.eval()

dummy_input = torch.zeros(1, 48000)  # 3s @ 16kHz

print(f"Exporting → {FP32_PATH}  (dynamo=False, opset=14)")
_cleanup(FP32_PATH)
torch.onnx.export(
    model,
    (dummy_input,),
    FP32_PATH,
    input_names=["input_values"],
    output_names=["logits"],
    dynamic_axes={
        "input_values": {0: "batch", 1: "num_samples"},
        "logits":       {0: "batch", 1: "time_steps"},
    },
    opset_version=14,
    do_constant_folding=True,
    dynamo=False,            # ← CỰC QUAN TRỌNG, xem comment đầu file
)
fp32_size_mb = os.path.getsize(FP32_PATH) / 1e6
print(f"  FP32 size: {fp32_size_mb:.1f} MB")
if fp32_size_mb < 100:
    raise SystemExit(
        f"FP32 export quá nhỏ ({fp32_size_mb:.1f}MB). Có vẻ weights bị "
        f"externalize. Kiểm tra: torch version, dynamo flag, hoặc xem có "
        f"file *.data sinh ra cạnh không."
    )
# Wav2Vec2 có ~317M params, FP32 = ~1.27GB là bình thường (file HF
# pytorch_model.bin nhỏ hơn vì lưu safetensors/half precision).

# ─────────────────────────────────────────────
# 2) Preprocess — chỉ constant fold + ONNX shape (KHÔNG symbolic shape).
#
#    Symbolic shape inference chết với dynamic axis `num_samples` của
#    Wav2Vec2 vì feature extractor có nhiều stride conv lồng nhau, sinh
#    biểu thức floor(floor(floor(num_samples/5)/2)/2 ...) mà ORT
#    SymbolicShapeInference không xử lý được:
#      "Exception: Incomplete symbolic shape inference"
#
#    quant_pre_process có flag `skip_symbolic_shape=True` để bỏ qua.
#    Với dynamic quantize (chỉ MatMul, không cần shape của activation),
#    không có symbolic shape vẫn quantize đúng.
# ─────────────────────────────────────────────
print(f"Preprocessing → {FP32_PREP_PATH}  (skip_symbolic_shape=True)")
quant_pre_process(
    FP32_PATH,
    FP32_PREP_PATH,
    skip_optimization=False,
    skip_onnx_shape=False,
    skip_symbolic_shape=True,   # ← bỏ qua, Wav2Vec2 dynamic axis vỡ chỗ này
)

# ─────────────────────────────────────────────
# 3) Dynamic-quantize MatMul ONLY
#    (Conv layers → giữ FP32 để tránh ConvInteger crash trên ORT-Android)
# ─────────────────────────────────────────────
print(f"Quantizing → {INT8_PATH}  (MatMul only, INT8)")
quantize_dynamic(
    FP32_PREP_PATH,
    INT8_PATH,
    weight_type=QuantType.QInt8,
    op_types_to_quantize=["MatMul"],
)
int8_size_mb = os.path.getsize(INT8_PATH) / 1e6
print(f"  INT8 size: {int8_size_mb:.1f} MB")
# Wav2Vec2-lv-60 = 317M params. MatMul quantize sang INT8 (1B/param),
# Conv giữ FP32 → kích thước thực tế ~310-360MB. Nếu > 600MB là quantize
# fail (MatMul không bị tìm thấy → vẫn FP32).
if int8_size_mb > 600:
    raise SystemExit(
        f"INT8 quá lớn ({int8_size_mb:.1f}MB) — quantize không tác dụng. "
        f"Kiểm tra quant_pre_process đã chạy chưa, hoặc op_types_to_quantize."
    )

# ─────────────────────────────────────────────
# 4) Verify: không còn ConvInteger / QLinearConv (sẽ crash trên ORT Android)
# ─────────────────────────────────────────────
import onnx
m = onnx.load(INT8_PATH)
bad = [n for n in m.graph.node if n.op_type in ("ConvInteger", "QLinearConv")]
if bad:
    print(f"\n!!! {len(bad)} unsupported quantized Conv ops:")
    for n in bad[:5]:
        print(f"    {n.op_type}  {n.name}")
    raise SystemExit("Model sẽ crash trên onnxruntime-android. Abort.")
print("OK — no ConvInteger/QLinearConv.")

# ─────────────────────────────────────────────
# 5) Tải vocab.json + special_tokens_map.json trực tiếp từ HF Hub,
#    KHÔNG đi qua Wav2Vec2PhonemeCTCTokenizer (cái đó yêu cầu phonemizer
#    chỉ để init backend espeak — ta không cần phonemize gì).
# ─────────────────────────────────────────────
print(f"Dumping tokenizer vocab → {VOCAB_PATH}")

vocab_file = hf_hub_download(MODEL_ID, "vocab.json")
specials_file = hf_hub_download(MODEL_ID, "special_tokens_map.json")

with open(vocab_file, "r", encoding="utf-8") as f:
    vocab_dict = json.load(f)   # {"<pad>": 0, "<s>": 1, ..., "a4": 391}

with open(specials_file, "r", encoding="utf-8") as f:
    specials_map = json.load(f)
    # {"unk_token":"<unk>","bos_token":"<s>","eos_token":"</s>","pad_token":"<pad>"}

vocab_size = max(vocab_dict.values()) + 1
id_to_token = ["" for _ in range(vocab_size)]
for token, idx in vocab_dict.items():
    id_to_token[idx] = token

# Special tokens phổ thông
special_tokens = set()
for k in ("pad_token", "bos_token", "eos_token", "unk_token"):
    v = specials_map.get(k)
    # v có thể là string hoặc dict {"content":"<pad>",...}
    if isinstance(v, dict):
        v = v.get("content")
    if v and v in vocab_dict:
        special_tokens.add(v)

special_ids = sorted({vocab_dict[t] for t in special_tokens})

# Word delimiter — tokenizer config có thể đặt key "word_delimiter_token".
# Model `wav2vec2-lv-60-espeak-cv-ft` KHÔNG có (Wav2Vec2PhonemeCTCTokenizer
# xuất phoneme phẳng), nhưng giữ logic cho linh hoạt.
word_delimiter_id = None
wdt = specials_map.get("word_delimiter_token")
if isinstance(wdt, dict):
    wdt = wdt.get("content")
if wdt and wdt in vocab_dict:
    word_delimiter_id = vocab_dict[wdt]

out = {
    "id_to_token":       id_to_token,
    "pad_id":            vocab_dict.get(
        specials_map.get("pad_token", "<pad>") if isinstance(specials_map.get("pad_token"), str)
        else specials_map["pad_token"]["content"],
        0,
    ),
    "special_ids":       special_ids,
    "word_delimiter_id": word_delimiter_id,   # null cho model này
}
with open(VOCAB_PATH, "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False, indent=2)
print(f"  vocab_size = {vocab_size}, pad_id = {out['pad_id']}, "
      f"word_delimiter_id = {out['word_delimiter_id']}")

# ─────────────────────────────────────────────
# 6) Dọn file trung gian
# ─────────────────────────────────────────────
_cleanup(FP32_PATH, FP32_PREP_PATH)

print("\n────────────────────────────────────────")
print("Hoàn tất. Copy 2 file sau vào app/src/main/assets/ :")
print(f"   {INT8_PATH}    ({int8_size_mb:.1f} MB)")
print(f"   {VOCAB_PATH}")
print("────────────────────────────────────────")
