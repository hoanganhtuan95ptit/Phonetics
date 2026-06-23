"""
Export + quantize facebook/wav2vec2-lv-60-espeak-cv-ft for Android ONNX Runtime.

Why this script exists:
  Default `quantize_dynamic` produces `ConvInteger` ops in the feature extractor.
  onnxruntime-android has no ConvInteger kernel → crashes at session creation:
    "Could not find an implementation for ConvInteger(10) node ..."

Fix:
  Quantize ONLY MatMul ops (skip Conv layers). Wav2Vec2 is mostly transformer
  MatMuls, so size drops from ~380MB → ~115MB and inference still works on
  onnxruntime-android.

Usage:
  pip install transformers torch onnx onnxruntime
  python export_wav2vec2_onnx.py

Output:
  wav2vec2_phoneme.onnx        (~380MB, FP32, intermediate)
  wav2vec2_phoneme_int8.onnx   (~115MB, ship this in assets/)
"""

import os
import torch
from transformers import Wav2Vec2ForCTC
from onnxruntime.quantization import quantize_dynamic, QuantType

MODEL_ID = "facebook/wav2vec2-lv-60-espeak-cv-ft"
FP32_PATH = "wav2vec2_phoneme.onnx"
INT8_PATH = "wav2vec2_phoneme_int8.onnx"

# 1) Export FP32 ONNX
print(f"Loading {MODEL_ID} ...")
model = Wav2Vec2ForCTC.from_pretrained(MODEL_ID)
model.eval()

dummy_input = torch.zeros(1, 48000)  # 3s @ 16kHz

print(f"Exporting → {FP32_PATH}")
torch.onnx.export(
    model,
    dummy_input,
    FP32_PATH,
    input_names=["input_values"],
    output_names=["logits"],
    dynamic_axes={
        "input_values": {0: "batch", 1: "num_samples"},
        "logits":       {0: "batch", 1: "time_steps"},
    },
    opset_version=14,
)
print(f"  FP32 size: {os.path.getsize(FP32_PATH) / 1e6:.1f} MB")

# 2) Dynamic-quantize MatMul ONLY (avoids ConvInteger crash on Android)
print(f"Quantizing → {INT8_PATH} (MatMul only)")
quantize_dynamic(
    FP32_PATH,
    INT8_PATH,
    weight_type=QuantType.QInt8,
    op_types_to_quantize=["MatMul"],
)
print(f"  INT8 size: {os.path.getsize(INT8_PATH) / 1e6:.1f} MB")

# 3) Verify: count any remaining ConvInteger / int-quantized Conv ops
import onnx
m = onnx.load(INT8_PATH)
# (IR version: onnxruntime-android >=1.25 hỗ trợ IR v10, không cần downgrade.)
bad = [n for n in m.graph.node if n.op_type in ("ConvInteger", "QLinearConv")]
if bad:
    print(f"\n!!! WARNING: {len(bad)} unsupported quantized Conv ops remain:")
    for n in bad[:5]:
        print(f"    {n.op_type}  {n.name}")
    raise SystemExit("Model will crash on onnxruntime-android. Aborting.")
print("\nOK — no ConvInteger/QLinearConv. Safe to ship in assets/.")
print(f"Copy {INT8_PATH} → app/src/main/assets/wav2vec2_phoneme.onnx")
