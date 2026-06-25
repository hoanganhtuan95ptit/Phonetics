/**
 * Wav2Vec2PhonemeRecognizer.kt
 *
 * Chạy model facebook/wav2vec2-lv-60-espeak-cv-ft qua OnnxRuntime Android
 * để chuyển audio float32 → chuỗi IPA phoneme.
 *
 *   Input:  float32[1, num_samples]  (16 kHz, mono, normalized zero-mean unit-var)
 *   Output: float32[1, time_steps, 392]  (logits trên vocab IPA)
 *   Decode: CTC greedy → list IPA phonemes
 *
 * Cách tải model:
 *   1. Chạy tools/export_wav2vec2_onnx.py — export ONNX + dump vocab.json
 *      thật của tokenizer (392 token).
 *   2. Copy 2 file vào assets/:
 *         wav2vec2_phoneme.onnx
 *         wav2vec2_phoneme_vocab.json
 */

package com.simple.feature.pronunciation_assessment.data.recognizer

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import com.simple.feature.pronunciation_assessment.domain.repositories.PhonemeRecognizer
import com.simple.phonetics.BRANCH
import java.nio.FloatBuffer

private const val DEFAULT_MODEL_URL =
    "https://github.com/hoanganhtuan95ptit/Phonetics/raw/refs/heads/{branch}/models/wav2vec2_phoneme.onnx"

private const val MODEL_FILE_NAME = "wav2vec2_phoneme.onnx"

class Wav2Vec2PhonemeRecognizer(
    private val context: Context,
    private val modelUrl: String = DEFAULT_MODEL_URL.replace("{branch}", BRANCH),
    private val modelFileName: String = MODEL_FILE_NAME,
    private val vocabFileName: String = Wav2Vec2Vocab.ASSET_FILE_NAME,
) : PhonemeRecognizer {

    private var ortEnv: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var vocab: Wav2Vec2Vocab? = null

    // ── Load model ────────────────────────────

    override suspend fun load(useGPU: Boolean, onProgress: ((percent: Int) -> Unit)?) {
        // Tải model từ URL về cacheDir, báo tiến trình qua onProgress
        val modelFile = ModelDownloader.downloadIfNeeded(
            url = modelUrl,
            cacheDir = context.cacheDir,
            fileName = modelFileName,
            onProgress = onProgress,
        )

        // Load vocab trước — nếu fail thì biết ngay (file thiếu trong assets)
        vocab = Wav2Vec2Vocab.loadFromAssets(context, vocabFileName)

        val env = OrtEnvironment.getEnvironment()
        ortEnv = env

        val sessionOptions = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(4)
            setInterOpNumThreads(1)
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)

            if (useGPU) {
                try { addNnapi() } catch (_: Exception) { /* fallback CPU */ }
            }
        }

        try {
            session = env.createSession(modelFile.absolutePath, sessionOptions)
        } catch (t: Throwable) {
            Log.d("tuanha", "load: ", t)
            modelFile.delete()
            throw t
        }
    }

    // ── Inference ─────────────────────────────

    override fun recognize(audioFloat: FloatArray): List<String> {
        val env = ortEnv ?: error("Chưa gọi load()")
        val session = this.session ?: error("Chưa gọi load()")
        val vocab = this.vocab ?: error("Chưa gọi load() — vocab chưa load")

        val inputShape = longArrayOf(1, audioFloat.size.toLong())
        OnnxTensor.createTensor(env, FloatBuffer.wrap(audioFloat), inputShape).use { inputTensor ->
            session.run(mapOf("input_values" to inputTensor)).use { results ->
                val logitsTensor = (results.firstOrNull { it.key == "logits" }?.value as? OnnxTensor)
                    ?: error("Output 'logits' not found")

                val shape = logitsTensor.info.shape // [1, time_steps, vocab_size]
                val totalElements = shape.fold(1L) { acc, l -> acc * l }.toInt()
                val logitsFlat = FloatArray(totalElements)
                logitsTensor.floatBuffer.get(logitsFlat)

                val vocabSize = shape[2].toInt()
                check(vocabSize == vocab.size) {
                    "Vocab size mismatch: model=$vocabSize  asset=${vocab.size}. " +
                        "Bạn đang dùng vocab.json không khớp model."
                }

                return CTCDecoder.greedyDecode(logitsFlat, vocabSize, vocab)
            }
        }
    }

    override fun close() {
        session?.close()
        ortEnv?.close()
        session = null
        ortEnv = null
        vocab = null
    }
}
