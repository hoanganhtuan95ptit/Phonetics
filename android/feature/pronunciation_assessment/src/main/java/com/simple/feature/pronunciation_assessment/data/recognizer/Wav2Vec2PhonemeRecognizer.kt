/**
 * Wav2Vec2PhonemeRecognizer.kt
 *
 * Chạy model facebook/wav2vec2-lv-60-espeak-cv-ft qua OnnxRuntime Android
 * để chuyển audio float32 → chuỗi IPA phoneme.
 *
 *   Input:  float32[1, num_samples]  (16 kHz, mono, normalized zero-mean unit-var)
 *   Output: float32[1, time_steps, 392]  (logits trên vocab IPA)
 *   Decode: CTC greedy → list IPA phonemes (+ frame timing nếu cần)
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
import com.simple.feature.pronunciation_assessment.domain.entities.PhonemeFrame
import com.simple.feature.pronunciation_assessment.domain.entities.RecognitionResult
import com.simple.feature.pronunciation_assessment.domain.repositories.PhonemeRecognizer
import com.simple.phonetics.BRANCH
import java.io.File
import java.nio.FloatBuffer
import kotlinx.coroutines.flow.collect

private const val LOG_TAG = "tuanha"
private const val INPUT_VALUES_NAME = "input_values"
private const val OUTPUT_LOGITS_NAME = "logits"
private const val VOCAB_SIZE_DIMENSION_INDEX = 2

/**
 * Frame stride của wav2vec2 base: convolutional encoder downsample 320×
 * @ 16 kHz → 20 ms / time step. Dùng để convert frame index → ms.
 */
private const val FRAME_MS = 20f

class Wav2Vec2PhonemeRecognizer(
    private val context: Context,
) : PhonemeRecognizer {

    private var ortEnv: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var vocab: Wav2Vec2Vocab? = null

    /**
     * Chuẩn bị đủ 3 thành phần bắt buộc cho inference:
     * model ONNX trên disk, vocab trong assets, và OrtSession đang mở.
     */
    override suspend fun load(
        useGPU: Boolean,
        onProgress: ((percent: Int) -> Unit)?,
        modelUrl: String,
        modelFileName: String,
        vocabFileName: String,
    ) {

        if (vocab != null) {

            return
        }

        val modelFile = downloadModel(modelUrl, modelFileName, onProgress)
        vocab = loadVocab(vocabFileName)
        val env = OrtEnvironment.getEnvironment()
        ortEnv = env
        session = createSession(env, modelFile, useGPU)
    }

    override fun recognize(audioFloat: FloatArray): List<String> =
        recognizeWithTiming(audioFloat).phonemes

    override fun recognizeWithTiming(audioFloat: FloatArray): RecognitionResult {

        val dependencies = requireLoadedDependencies()
        val frames = runInference(audioFloat, dependencies)

        // Scorer cần cả chuỗi phoneme phẳng và frame timing để chấm vowel length.
        return RecognitionResult(
            phonemes = frames.map(PhonemeFrame::phoneme),
            frames = frames,
        )
    }

    override fun close() {

        session?.close()
        ortEnv?.close()
        session = null
        ortEnv = null
        vocab = null
    }

    /**
     * Model lớn nên luôn đi qua cache file; không đọc nguyên ONNX vào RAM.
     */
    private suspend fun downloadModel(
        modelUrl: String,
        modelFileName: String,
        onProgress: ((Int) -> Unit)?,
    ): File {

        var downloadedFile: File? = null

        ModelDownloader.downloadIfNeeded(
            url = modelUrl,
            cacheDir = context.cacheDir,
            fileName = modelFileName,
        ).collect { event ->

            when (event) {

                // Downloader phát progress và file kết quả trên cùng Flow:
                // progress để UI hiển thị, Completed để bước tạo OrtSession dùng file thật.
                is ModelDownloadEvent.Completed -> downloadedFile = event.file
                is ModelDownloadEvent.Progress -> onProgress?.invoke(event.percent)
            }
        }

        return downloadedFile ?: error("Model download did not complete")
    }

    /**
     * Vocab phải khớp đúng model export, nếu lệch size thì decode token sẽ sai.
     */
    private fun loadVocab(vocabFileName: String): Wav2Vec2Vocab {

        return Wav2Vec2Vocab.loadFromAssets(context, vocabFileName)
    }

    /**
     * Tạo OrtSession từ file path để OnnxRuntime tự quản lý phần đọc model.
     */
    private fun createSession(env: OrtEnvironment, modelFile: File, useGPU: Boolean): OrtSession {

        val sessionOptions = createSessionOptions(useGPU)
        return try {

            env.createSession(modelFile.absolutePath, sessionOptions)
        } catch (t: Throwable) {

            // Xóa cache lỗi để lần load sau có thể tải lại model sạch.
            Log.d(LOG_TAG, "load: ", t)
            modelFile.delete()
            throw t
        }
    }

    /**
     * Giữ số thread cố định để inference ổn định hơn trên nhiều thiết bị Android.
     */
    private fun createSessionOptions(useGPU: Boolean): OrtSession.SessionOptions {

        return OrtSession.SessionOptions().apply {

            setIntraOpNumThreads(4)
            setInterOpNumThreads(1)
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            enableNnapiIfNeeded(useGPU)
        }
    }

    /**
     * NNAPI là tối ưu best-effort: thiết bị không hỗ trợ thì quay về CPU.
     */
    private fun OrtSession.SessionOptions.enableNnapiIfNeeded(useGPU: Boolean) {

        if (!useGPU) {

            return
        }

        try {

            addNnapi()
        } catch (_: Exception) {

            // Fall back to CPU when NNAPI is unavailable on the device.
        }
    }

    /**
     * Tập trung null-check ở một chỗ để các bước inference phía sau luôn non-null.
     */
    private fun requireLoadedDependencies(): LoadedDependencies {

        return LoadedDependencies(
            env = ortEnv ?: error("Chưa gọi load()"),
            session = session ?: error("Chưa gọi load()"),
            vocab = vocab ?: error("Chưa gọi load() — vocab chưa load"),
        )
    }

    /**
     * Audio đầu vào đã được chuẩn hóa ở tầng recorder/repository trước khi vào recognizer.
     */
    private fun runInference(audioFloat: FloatArray, dependencies: LoadedDependencies): List<PhonemeFrame> {

        val inputShape = longArrayOf(1, audioFloat.size.toLong())
        val inputBuffer = FloatBuffer.wrap(audioFloat)
        OnnxTensor.createTensor(dependencies.env, inputBuffer, inputShape).use { inputTensor ->

            return runSession(inputTensor, dependencies.session, dependencies.vocab)
        }
    }

    /**
     * Mapping input name phải trùng với tên tensor khi export ONNX từ wav2vec2.
     */
    private fun runSession(
        inputTensor: OnnxTensor,
        session: OrtSession,
        vocab: Wav2Vec2Vocab,
    ): List<PhonemeFrame> {

        session.run(mapOf(INPUT_VALUES_NAME to inputTensor)).use { results ->

            val logitsTensor = results.findLogitsTensor()
            return decodeLogits(logitsTensor, vocab)
        }
    }

    /**
     * Export script đặt output là "logits"; nếu tên này đổi thì fail sớm tại đây.
     */
    private fun OrtSession.Result.findLogitsTensor(): OnnxTensor {

        return (firstOrNull { result ->

            result.key == OUTPUT_LOGITS_NAME
        }?.value as? OnnxTensor) ?: error("Output '$OUTPUT_LOGITS_NAME' not found")
    }

    /**
     * Logits có shape [1, time_steps, vocab_size], CTCDecoder nhận dạng flat array.
     */
    private fun decodeLogits(logitsTensor: OnnxTensor, vocab: Wav2Vec2Vocab): List<PhonemeFrame> {

        val shape = logitsTensor.info.shape
        val logitsFlat = logitsTensor.readFlatLogits(shape)
        val vocabSize = shape[VOCAB_SIZE_DIMENSION_INDEX].toInt()
        checkVocabSize(vocabSize, vocab)

        return CTCDecoder.greedyDecodeWithTiming(
            logits = logitsFlat,
            vocabSize = vocabSize,
            vocab = vocab,
            frameMs = FRAME_MS,
        )
    }

    /**
     * Copy FloatBuffer ra FloatArray vì decoder hiện xử lý theo index tuyến tính.
     */
    private fun OnnxTensor.readFlatLogits(shape: LongArray): FloatArray {

        val logitsFlat = FloatArray(shape.totalElementCount())
        floatBuffer.get(logitsFlat)
        return logitsFlat
    }

    /**
     * Tính tổng số phần tử từ shape ONNX để cấp đúng kích thước buffer đọc logits.
     */
    private fun LongArray.totalElementCount(): Int {

        var totalElements = 1L
        for (dimension in this) {

            totalElements *= dimension
        }
        return totalElements.toInt()
    }

    /**
     * Fail sớm khi model/vocab không cùng phiên bản thay vì decode ra IPA sai âm thầm.
     */
    private fun checkVocabSize(vocabSize: Int, vocab: Wav2Vec2Vocab) {

        check(vocabSize == vocab.size) {

            "Vocab size mismatch: model=$vocabSize  asset=${vocab.size}. " +
                "Bạn đang dùng vocab.json không khớp model."
        }
    }

    companion object {

        const val MODEL_FILE_NAME = "wav2vec2_phoneme.onnx"
        const val VOCAB_FILE_NAME = "wav2vec2_phoneme_vocab.json"

        fun defaultModelUrl(): String =
            DEFAULT_MODEL_URL_TEMPLATE.replace("{branch}", BRANCH)

        private const val DEFAULT_MODEL_URL_TEMPLATE =
            "https://github.com/hoanganhtuan95ptit/Phonetics/raw/refs/heads/{branch}/models/wav2vec2_phoneme.onnx"
    }
}

private data class LoadedDependencies(
    val env: OrtEnvironment,
    val session: OrtSession,
    val vocab: Wav2Vec2Vocab,
)
