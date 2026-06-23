/**
 * Wav2Vec2Phoneme.kt
 *
 * Chạy model wav2vec2-lv-60-espeak-cv-ft qua OnnxRuntime Android
 * để chuyển audio float32 → chuỗi IPA phoneme.
 *
 * Model: facebook/wav2vec2-lv-60-espeak-cv-ft
 *   - Input:  float32[1, num_samples]  (16kHz, mono, normalized [-1,1])
 *   - Output: float32[1, time_steps, 392]  (logits trên vocab IPA)
 *   - Decode: CTC greedy → list IPA phonemes
 *
 * Cách tải model:
 *   1. Download từ HuggingFace: https://huggingface.co/facebook/wav2vec2-lv-60-espeak-cv-ft
 *   2. Export sang ONNX: python export_wav2vec2_onnx.py  (xem cuối file)
 *   3. Bỏ file wav2vec2_phoneme.onnx vào assets/
 *
 * build.gradle:
 *   implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.26.0'
 *
 * AndroidManifest.xml:
 *   <uses-permission android:name="android.permission.RECORD_AUDIO"/>
 */

package com.simple.phonetics.ui.speak.services.pronunciation_assessment.data.use_case

import ai.onnxruntime.*
import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer

// ─────────────────────────────────────────────
// IPA Vocabulary
// ─────────────────────────────────────────────

/**
 * Vocabulary của wav2vec2-lv-60-espeak-cv-ft.
 * Index → IPA symbol.
 *
 * Special tokens:
 *   0 = <pad>  — CTC blank token, bỏ qua khi decode
 *   4 = |      — word boundary
 *
 * Nguồn: tokenizer.json từ HuggingFace model card.
 */
object Wav2Vec2Vocab {

    val IPA_TOKENS: List<String> = listOf(
        "<pad>", "<s>", "</s>", "<unk>", "|",
        // Consonants
        "p", "b", "t", "d", "k", "ɡ",
        "f", "v", "θ", "ð", "s", "z", "ʃ", "ʒ", "h",
        "tʃ", "dʒ",
        "m", "n", "ŋ",
        "l", "r", "w", "j",
        // Vowels
        "iː", "ɪ", "uː", "ʊ",
        "eɪ", "e", "ɛ", "ə", "ɜː", "ʌ",
        "oʊ", "ɔː", "ɔ",
        "æ", "ɑː", "a",
        "aɪ", "aʊ", "ɔɪ",
        // Extended IPA (các ngôn ngữ khác trong training data)
        "ɐ", "ɑ", "ɒ", "ø", "œ", "y", "ʏ", "ɯ",
        "ç", "x", "ɣ", "χ", "ħ", "ʕ", "ʔ",
        "ɬ", "ɮ", "ʎ", "ɹ", "ɻ", "ʀ", "ʁ",
        "ɱ", "ɳ", "ɲ", "ʙ", "ʜ",
        "ˈ", "ˌ", "ː",   // stress markers
        // ... (392 tokens total — truncated for readability)
    )

    private val tokenToIndex: Map<String, Int> by lazy {
        IPA_TOKENS.mapIndexed { i, t -> t to i }.toMap()
    }

    val PAD_ID = 0
    val WORD_BOUNDARY_ID = 4   // "|"

    fun decode(id: Int): String? = IPA_TOKENS.getOrNull(id)
    fun encode(token: String): Int = tokenToIndex[token] ?: 3  // <unk>

    /** Lọc các token là âm vị thực sự (bỏ special tokens và markers) */
    fun isPhoneme(token: String): Boolean =
        token !in setOf("<pad>", "<s>", "</s>", "<unk>", "|", "ˈ", "ˌ", "ː")
}

// ─────────────────────────────────────────────
// CTC Decoder
// ─────────────────────────────────────────────

/**
 * CTCDecoder — chuyển logits → IPA phoneme list.
 *
 * CTC Greedy Decode:
 *   1. Với mỗi time step, lấy argmax → token có xác suất cao nhất
 *   2. Collapse consecutive duplicates: [k,k,k,æ,æ,t] → [k,æ,t]
 *   3. Bỏ <pad> (blank token)
 *   4. Bỏ word boundary "|" (optional — giữ lại để biết ranh giới từ)
 *
 * Ví dụ với "cat":
 *   logits argmax: [0,0,9,9,9,0,35,35,0,7,7,0]
 *   collapse dup:  [0,9,0,35,0,7,0]
 *   remove pad:    [9, 35, 7]  →  ["k", "æ", "t"]
 */
object CTCDecoder {

    /**
     * @param logits Float32 array shape [time_steps × vocab_size]
     * @param vocabSize kích thước vocab (thường 392)
     * @return list IPA phonemes
     */
    fun greedyDecode(logits: FloatArray, vocabSize: Int): List<String> {
        val timeSteps = logits.size / vocabSize
        val tokens = mutableListOf<String>()
        var prevId = -1

        for (t in 0 until timeSteps) {
            // Argmax trên vocab dimension
            var maxId = 0
            var maxVal = logits[t * vocabSize]
            for (v in 1 until vocabSize) {
                val val_ = logits[t * vocabSize + v]
                if (val_ > maxVal) { maxVal = val_; maxId = v }
            }

            // Collapse duplicates + bỏ pad
            if (maxId != prevId && maxId != Wav2Vec2Vocab.PAD_ID) {
                val token = Wav2Vec2Vocab.decode(maxId)
                if (token != null && Wav2Vec2Vocab.isPhoneme(token)) {
                    tokens.add(token)
                }
            }
            prevId = maxId
        }

        return tokens
    }

    /**
     * Decode có word boundary — trả về list<list<string>>
     * mỗi inner list là phonemes của 1 từ.
     * Dùng để map phoneme về từng từ khi chấm điểm.
     */
    fun greedyDecodeWithBoundaries(logits: FloatArray, vocabSize: Int): List<List<String>> {
        val timeSteps = logits.size / vocabSize
        val words = mutableListOf<MutableList<String>>()
        var current = mutableListOf<String>()
        var prevId = -1

        for (t in 0 until timeSteps) {
            var maxId = 0
            var maxVal = logits[t * vocabSize]
            for (v in 1 until vocabSize) {
                val v_ = logits[t * vocabSize + v]
                if (v_ > maxVal) { maxVal = v_; maxId = v }
            }

            if (maxId != prevId) {
                when {
                    maxId == Wav2Vec2Vocab.WORD_BOUNDARY_ID -> {
                        if (current.isNotEmpty()) {
                            words.add(current)
                            current = mutableListOf()
                        }
                    }
                    maxId != Wav2Vec2Vocab.PAD_ID -> {
                        val token = Wav2Vec2Vocab.decode(maxId)
                        if (token != null && Wav2Vec2Vocab.isPhoneme(token)) {
                            current.add(token)
                        }
                    }
                }
            }
            prevId = maxId
        }

        if (current.isNotEmpty()) words.add(current)
        return words
    }
}

// ─────────────────────────────────────────────
// Wav2Vec2Phoneme — ONNX inference
// ─────────────────────────────────────────────

/**
 * Wav2Vec2Phoneme — wrapper cho OnnxRuntime inference trên Android.
 *
 * Lifecycle:
 *   val recognizer = Wav2Vec2Phoneme(context)
 *   recognizer.load()           // gọi 1 lần khi app start
 *   val phonemes = recognizer.recognize(floatArray)
 *   recognizer.close()          // khi không dùng nữa
 */
class Wav2Vec2Phoneme(private val context: Context) : AutoCloseable {

    private var ortEnv: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var vocabSize: Int = Wav2Vec2Vocab.IPA_TOKENS.size

    // ── Load model ────────────────────────────

    /**
     * Load model từ assets vào OnnxRuntime.
     * Gọi trong coroutine hoặc background thread — blocking ~200ms.
     *
     * @param modelFileName tên file .onnx trong assets/
     * @param useGPU        dùng NNAPI (Neural Network API) trên Android
     */
    fun load(
        modelFileName: String = "wav2vec2_phoneme.onnx",
        useGPU: Boolean = false
    ) {
        ortEnv = OrtEnvironment.getEnvironment()

        val sessionOptions = OrtSession.SessionOptions().apply {
            // Inter/intra op parallelism — điều chỉnh theo số CPU core
            setIntraOpNumThreads(4)
            setInterOpNumThreads(1)

            // Optimization level: ALL cho inference nhanh nhất
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)

            if (useGPU) {
                // NNAPI = Android Neural Networks API — accelerate trên NPU/GPU
                // Fallback về CPU nếu NNAPI không available
                try {
                    addNnapi()
                } catch (e: Exception) {
                    // NNAPI không available trên thiết bị này, dùng CPU
                }
            }
        }

        // Copy model từ assets ra file cache (stream theo chunk để tránh OOM
        // — model ~300MB sẽ ném OOM nếu đọc nguyên vào byte array).
        // Sau đó load session trực tiếp từ đường dẫn file → ORT mmap nội bộ.
        val modelFile = copyAssetToCacheIfNeeded(modelFileName)
        try {
            session = ortEnv!!.createSession(modelFile.absolutePath, sessionOptions)
        } catch (t: Throwable) {
            // Xoá cache để lần load sau ép copy lại từ assets
            // (tránh kẹt file model cũ bị lỗi).
            modelFile.delete()
            throw t
        }
    }

    /**
     * Stream asset → file trong cacheDir. Bỏ qua nếu file đã tồn tại và cùng kích thước
     * (asset size có sẵn qua AssetFileDescriptor.length).
     */
    private fun copyAssetToCacheIfNeeded(assetName: String): File {
        val outFile = File(context.cacheDir, assetName)

        val expectedSize: Long = try {
            context.assets.openFd(assetName).use { it.length }
        } catch (_: Exception) {
            // Asset bị nén (không openFd được) → không biết size trước, sẽ copy lại
            -1L
        }

        if (outFile.exists() && expectedSize > 0 && outFile.length() == expectedSize) {
            return outFile
        }

        context.assets.open(assetName).use { input ->
            FileOutputStream(outFile).use { output ->
                val buffer = ByteArray(64 * 1024) // 64KB chunks
                while (true) {
                    val n = input.read(buffer)
                    if (n <= 0) break
                    output.write(buffer, 0, n)
                }
                output.fd.sync()
            }
        }
        return outFile
    }

    // ── Inference ─────────────────────────────

    /**
     * Nhận dạng phoneme từ audio.
     *
     * @param audioFloat  PCM float32 [-1, 1], 16kHz, mono
     * @return list IPA phonemes, ví dụ ["h", "ɛ", "l", "oʊ"]
     */
    fun recognize(audioFloat: FloatArray): List<String> {
        val env     = ortEnv     ?: error("Chưa gọi load()")
        val session = this.session ?: error("Chưa gọi load()")

        // Tạo input tensor shape [1, num_samples]
        val inputShape  = longArrayOf(1, audioFloat.size.toLong())
        val inputTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(audioFloat),
            inputShape
        )

        // Run inference
        val results = session.run(mapOf("input_values" to inputTensor))

        // Lấy output logits — shape [1, time_steps, vocab_size]
        var logitsTensor: OnnxTensor? = null
        for (entry in results) {
            if (entry.key == "logits") {
                logitsTensor = entry.value as OnnxTensor
                break
            }
        }
        val tensor = logitsTensor ?: error("Output 'logits' not found")

        val shape = tensor.info.shape
        val totalElements = shape.reduce { acc, l -> acc * l }.toInt()
        val logitsFlat = FloatArray(totalElements)
        tensor.floatBuffer.get(logitsFlat)

        // Tính vocab size từ shape thực tế
        vocabSize = shape[2].toInt()

        // CTC decode → IPA phonemes
        return CTCDecoder.greedyDecode(logitsFlat, vocabSize)
    }

    /**
     * Nhận dạng với word boundaries.
     * Trả về phonemes được nhóm theo từ.
     */
    fun recognizeWithBoundaries(audioFloat: FloatArray): List<List<String>> {
        val env     = ortEnv     ?: error("Chưa gọi load()")
        val session = this.session ?: error("Chưa gọi load()")

        val inputShape  = longArrayOf(1, audioFloat.size.toLong())
        val inputTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(audioFloat),
            inputShape
        )

        val results      = session.run(mapOf("input_values" to inputTensor))

        var logitsTensor: OnnxTensor? = null
        for (entry in results) {
            if (entry.key == "logits") {
                logitsTensor = entry.value as OnnxTensor
                break
            }
        }
        val tensor = logitsTensor ?: error("Output 'logits' not found")

        val shape        = tensor.info.shape
        val totalElements = shape.reduce { acc, l -> acc * l }.toInt()
        val logitsFlat   = FloatArray(totalElements)
        tensor.floatBuffer.get(logitsFlat)

        vocabSize        = shape[2].toInt()

        return CTCDecoder.greedyDecodeWithBoundaries(logitsFlat, vocabSize)
    }

    override fun close() {
        session?.close()
        ortEnv?.close()
    }
}

/*
 * ─────────────────────────────────────────────
 * Script export ONNX (chạy trên máy tính, không phải Android)
 * ─────────────────────────────────────────────
 *
 * File: export_wav2vec2_onnx.py
 *
 * pip install transformers torch onnx onnxruntime
 *
 * from transformers import Wav2Vec2ForCTC, Wav2Vec2Processor
 * import torch
 *
 * MODEL_ID = "facebook/wav2vec2-lv-60-espeak-cv-ft"
 * model    = Wav2Vec2ForCTC.from_pretrained(MODEL_ID)
 * model.eval()
 *
 * # Dummy input: 3 giây audio tại 16kHz
 * dummy_input = torch.zeros(1, 48000)
 *
 * torch.onnx.export(
 *     model,
 *     dummy_input,
 *     "wav2vec2_phoneme.onnx",
 *     input_names  = ["input_values"],
 *     output_names = ["logits"],
 *     dynamic_axes = {
 *         "input_values": {0: "batch", 1: "num_samples"},
 *         "logits":       {0: "batch", 1: "time_steps"},
 *     },
 *     opset_version = 14,
 * )
 * print("Exported! Size:", os.path.getsize("wav2vec2_phoneme.onnx") / 1e6, "MB")
 * # Output: ~380MB (full model) hoặc ~95MB (quantized int8)
 *
 * # Quantize để giảm kích thước (optional nhưng khuyên dùng)
 * from onnxruntime.quantization import quantize_dynamic, QuantType
 * quantize_dynamic(
 *     "wav2vec2_phoneme.onnx",
 *     "wav2vec2_phoneme.onnx",
 *     weight_type = QuantType.QInt8,
 *     op_types_to_quantize = ["MatMul"],   # QUAN TRỌNG: chỉ quantize MatMul.
 *                                          # Mặc định sẽ quantize cả Conv → tạo ra
 *                                          # ConvInteger op mà onnxruntime-android
 *                                          # KHÔNG có kernel → crash khi load.
 * )
 * # Kết quả: ~110-120MB, chạy ngon trên Android.
 */
