/**
 * Wav2Vec2SpeechRecognizer.kt
 *
 * Chuyển đổi audio → văn bản (ASR) offline dùng model Wav2Vec2 qua OnnxRuntime Android.
 * Giao diện bắt chước Android SpeechRecognizer + RecognitionListener để dễ thay thế.
 *
 * ┌────────────────────────────────────────────────────────┐
 * │  Google SpeechRecognizer   →   Wav2Vec2SpeechRecognizer │
 * │  (online, cần internet)    →   (offline, chạy on-device) │
 * └────────────────────────────────────────────────────────┘
 *
 * Model đề xuất (English): facebook/wav2vec2-base-960h
 *   - Vocab : 32 tokens  (<pad> | <s> | </s> | <unk> | | | A-Z | ')
 *   - Input : float32[1, num_samples]  (16 kHz, mono, normalized [-1,1])
 *   - Output: float32[1, time_steps, 32]
 *
 * Model đề xuất (Multilingual): facebook/wav2vec2-large-xlsr-53-*
 *   Mỗi ngôn ngữ có vocab riêng — truyền customVocab khi khởi tạo.
 *
 * Cách export ONNX: xem khối comment cuối file.
 *
 * Cách dùng:
 *   val asr = Wav2Vec2SpeechRecognizer(context)
 *   asr.onReadyForSpeech    = { /* mic sẵn sàng  */ }
 *   asr.onBeginningOfSpeech = { /* bắt đầu nói   */ }
 *   asr.onRmsChanged        = { rms -> showVolume(rms) }
 *   asr.onEndOfSpeech       = { /* ngừng nói     */ }
 *   asr.onPartialResults    = { text -> showPartial(text) }
 *   asr.onResults           = { text -> useText(text) }
 *   asr.onError             = { code, msg -> handleError(code, msg) }
 *
 *   lifecycleScope.launch { asr.load() }   // gọi 1 lần, background thread
 *   asr.startListening()
 *   // ... tự dừng khi im lặng > SILENCE_TIMEOUT_MS, hoặc:
 *   asr.stopListening()
 *   asr.destroy()
 *
 * build.gradle:
 *   implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.26.0'
 *
 * AndroidManifest.xml:
 *   <uses-permission android:name="android.permission.RECORD_AUDIO"/>
 */

package com.simple.phonetics.ui.speak.services.pronunciation_assessment.data.use_case

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer

// ─────────────────────────────────────────────
// ASR Vocabulary  (facebook/wav2vec2-base-960h)
// ─────────────────────────────────────────────

/**
 * Vocabulary mặc định cho model `facebook/wav2vec2-base-960h` (English).
 *
 * Nguồn: https://huggingface.co/facebook/wav2vec2-base-960h/raw/main/vocab.json
 *
 * Special tokens:
 *   0 = <pad>   — CTC blank, bỏ khi decode
 *   1 = <s>
 *   2 = </s>
 *   3 = <unk>
 *   4 = |       — word boundary, chuyển thành space " "
 *   5..31 = E T A O N I H S R D L U M W C F G Y P B V K ' X J Q Z
 *
 * Để dùng model ngôn ngữ khác, truyền [customVocab] vào constructor
 * Wav2Vec2SpeechRecognizer. Các thứ tự token phải khớp với vocab.json
 * của model tương ứng.
 */
object Wav2Vec2ASRVocab {

    /** Token mặc định — English wav2vec2-base-960h */
    val DEFAULT_TOKENS: List<String> = listOf(
        "<pad>", "<s>", "</s>", "<unk>", "|",
        "E", "T", "A", "O", "N", "I", "H", "S", "R", "D",
        "L", "U", "M", "W", "C", "F", "G", "Y", "P", "B",
        "V", "K", "'", "X", "J", "Q", "Z"
    )

    const val PAD_ID           = 0
    const val WORD_BOUNDARY    = "|"

    /**
     * Token word-boundary — index 4 trong vocab mặc định.
     * Sẽ được chuyển thành khoảng trắng " " khi decode thành text.
     */
    fun wordBoundaryId(tokens: List<String>): Int = tokens.indexOf(WORD_BOUNDARY)
}

// ─────────────────────────────────────────────
// CTC Text Decoder
// ─────────────────────────────────────────────

/**
 * CTCTextDecoder — chuyển logits từ Wav2Vec2 ASR → văn bản.
 *
 * Thuật toán CTC Greedy Decode:
 *   1. Mỗi time step: argmax → token có xác suất cao nhất
 *   2. Collapse consecutive duplicates: [T,T,H,H,E,E] → [T,H,E]
 *   3. Bỏ <pad> (blank token, index 0)
 *   4. "|" → " " (word boundary → space)
 *   5. Ghép tất cả ký tự → string kết quả
 *
 * Ví dụ với "hello":
 *   logits argmax : [4,11,5,15,15,19,4]   // |,H,E,L,L,O,|
 *   collapse dup  : [4,11,5,15,19,4]
 *   remove pad    : [4,11,5,15,19,4]
 *   decode        : " hello "  →  trim → "HELLO"  →  lowercase → "hello"
 */
object CTCTextDecoder {

    /**
     * Decode logits → văn bản.
     *
     * @param logits    Float32 flat array, shape [time_steps × vocab_size]
     * @param vocabSize kích thước vocab (32 với wav2vec2-base-960h)
     * @param tokens    danh sách token (index → string)
     * @return văn bản đã nhận dạng, lowercase, đã trim
     */
    fun greedyDecode(
        logits: FloatArray,
        vocabSize: Int,
        tokens: List<String>
    ): String {
        val timeSteps = logits.size / vocabSize
        val sb = StringBuilder()
        var prevId = -1
        val wordBoundaryId = Wav2Vec2ASRVocab.wordBoundaryId(tokens)

        for (t in 0 until timeSteps) {
            // Argmax trên vocab dimension
            var maxId = 0
            var maxVal = logits[t * vocabSize]
            for (v in 1 until vocabSize) {
                val v_ = logits[t * vocabSize + v]
                if (v_ > maxVal) { maxVal = v_; maxId = v }
            }

            // Collapse duplicates + bỏ pad
            if (maxId != prevId && maxId != Wav2Vec2ASRVocab.PAD_ID) {
                when {
                    maxId == wordBoundaryId -> sb.append(' ')
                    maxId < tokens.size -> sb.append(tokens[maxId])
                }
            }
            prevId = maxId
        }

        return sb.toString().trim().lowercase()
    }

    /**
     * Decode với thông tin confidence per-token (softmax score).
     * Trả về cặp (text, averageConfidence).
     *
     * averageConfidence ∈ [0, 1] — xác suất trung bình của các token được chọn.
     * Dùng để quyết định có emit partial result không (bỏ qua nếu confidence thấp).
     */
    fun greedyDecodeWithConfidence(
        logits: FloatArray,
        vocabSize: Int,
        tokens: List<String>
    ): Pair<String, Float> {
        val timeSteps = logits.size / vocabSize
        val sb = StringBuilder()
        var prevId = -1
        var totalConfidence = 0f
        var tokenCount = 0
        val wordBoundaryId = Wav2Vec2ASRVocab.wordBoundaryId(tokens)

        for (t in 0 until timeSteps) {
            // Argmax + softmax confidence
            var maxId = 0
            var maxVal = logits[t * vocabSize]
            for (v in 1 until vocabSize) {
                val v_ = logits[t * vocabSize + v]
                if (v_ > maxVal) { maxVal = v_; maxId = v }
            }

            // Softmax confidence cho token được chọn
            var expSum = 0f
            for (v in 0 until vocabSize) {
                expSum += Math.exp((logits[t * vocabSize + v] - maxVal).toDouble()).toFloat()
            }
            val confidence = 1f / expSum  // exp(0) / sum = 1 / expSum

            if (maxId != prevId && maxId != Wav2Vec2ASRVocab.PAD_ID) {
                when {
                    maxId == wordBoundaryId -> sb.append(' ')
                    maxId < tokens.size -> {
                        sb.append(tokens[maxId])
                        totalConfidence += confidence
                        tokenCount++
                    }
                }
            }
            prevId = maxId
        }

        val avgConf = if (tokenCount > 0) totalConfidence / tokenCount else 0f
        return sb.toString().trim().lowercase() to avgConf
    }
}

// ─────────────────────────────────────────────
// Wav2Vec2SpeechRecognizer
// ─────────────────────────────────────────────

/**
 * Wav2Vec2SpeechRecognizer — offline speech-to-text, giao diện giống
 * [android.speech.SpeechRecognizer] + [android.speech.RecognitionListener].
 *
 * Điểm khác biệt so với Google SpeechRecognizer:
 *   ✓ Hoàn toàn offline, không cần internet
 *   ✓ Không gọi Google server, không gửi audio ra ngoài
 *   ✓ Tích hợp VAD (Voice Activity Detection) nội bộ
 *   ✗ Độ chính xác thấp hơn Google cho tiếng Anh thông thường
 *   ✗ Cần file model ONNX (~100MB sau quantize) trong assets/
 *
 * Error codes tương thích với [SpeechRecognizer]:
 *   ERROR_AUDIO           = 3
 *   ERROR_CLIENT          = 5
 *   ERROR_SPEECH_TIMEOUT  = 6
 *   ERROR_NO_MATCH        = 7
 *   ERROR_SERVER          = 4  (dùng cho model load failure)
 *
 * @param context       Android Context
 * @param customVocab   Vocab tùy chỉnh cho model khác ngôn ngữ.
 *                      Null = dùng [Wav2Vec2ASRVocab.DEFAULT_TOKENS] (English).
 */
class Wav2Vec2SpeechRecognizer(
    private val context: Context,
    private val customVocab: List<String>? = null
) : AutoCloseable {

    // ── Callbacks (giống RecognitionListener) ─

    /** Mic đã sẵn sàng, bắt đầu lắng nghe */
    var onReadyForSpeech: (() -> Unit)? = null

    /** Phát hiện giọng nói bắt đầu */
    var onBeginningOfSpeech: (() -> Unit)? = null

    /**
     * Cường độ âm thanh thay đổi (RMS dB, range ~0–10).
     * Dùng để animate waveform / volume indicator trên UI.
     */
    var onRmsChanged: ((rmsdB: Float) -> Unit)? = null

    /** Người dùng đã ngừng nói (im lặng phát hiện) */
    var onEndOfSpeech: (() -> Unit)? = null

    /**
     * Kết quả trung gian — cập nhật realtime mỗi 500ms khi đang nói.
     * Tương đương [android.speech.RecognitionListener.onPartialResults].
     */
    var onPartialResults: ((text: String) -> Unit)? = null

    /**
     * Kết quả cuối cùng sau khi người dùng dừng nói.
     * Tương đương [android.speech.RecognitionListener.onResults].
     */
    var onResults: ((text: String) -> Unit)? = null

    /**
     * Lỗi xảy ra.
     * @param errorCode tương thích với hằng số [SpeechRecognizer.ERROR_*]
     * @param message   mô tả lỗi (tiếng Việt)
     */
    var onError: ((errorCode: Int, message: String) -> Unit)? = null

    // ── Internal state ────────────────────────

    private val tokens: List<String> = customVocab ?: Wav2Vec2ASRVocab.DEFAULT_TOKENS
    private val vocabSize get() = tokens.size

    private var ortEnv: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var isLoaded = false

    private val audioProcessor = AudioProcessor(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Ngưỡng confidence tối thiểu để emit partial result
    private val minPartialConfidence = 0.3f

    // ── Model lifecycle ───────────────────────

    /**
     * Load model ONNX từ assets vào OnnxRuntime.
     * Gọi 1 lần trên background thread — blocking ~200–400ms.
     *
     * @param modelFileName tên file .onnx trong thư mục assets/
     * @param useGPU        dùng Android NNAPI acceleration nếu có
     * @throws Exception    nếu file không tồn tại hoặc model không hợp lệ
     */
    suspend fun load(
        modelFileName: String = "wav2vec2_asr.onnx",
        useGPU: Boolean = false
    ) = withContext(Dispatchers.IO) {
        try {
            ortEnv = OrtEnvironment.getEnvironment()

            val opts = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
                setInterOpNumThreads(1)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                if (useGPU) {
                    runCatching { addNnapi() }  // fallback CPU nếu NNAPI không có
                }
            }

            val modelFile = copyAssetToCacheIfNeeded(modelFileName)
            session = ortEnv!!.createSession(modelFile.absolutePath, opts)
            isLoaded = true
        } catch (t: Throwable) {
            withContext(Dispatchers.Main) {
                onError?.invoke(
                    SpeechRecognizer.ERROR_SERVER,
                    "Không load được model: ${t.message}"
                )
            }
        }
    }

    /**
     * Stream asset → file cache. Bỏ qua nếu file đã tồn tại và đúng kích thước.
     * Dùng 64KB chunks để tránh OOM với model lớn (~100–380MB).
     */
    private fun copyAssetToCacheIfNeeded(assetName: String): File {
        val outFile = File(context.cacheDir, assetName)

        val expectedSize: Long = runCatching {
            context.assets.openFd(assetName).use { it.length }
        }.getOrElse { -1L }

        if (outFile.exists() && expectedSize > 0 && outFile.length() == expectedSize) {
            return outFile
        }

        context.assets.open(assetName).use { input ->
            FileOutputStream(outFile).use { output ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    output.write(buf, 0, n)
                }
                output.fd.sync()
            }
        }
        return outFile
    }

    // ── Listening control ─────────────────────

    /**
     * Bắt đầu lắng nghe microphone và nhận dạng giọng nói.
     *
     * Thứ tự callbacks phát ra (giống Google SpeechRecognizer):
     *   1. onReadyForSpeech     — mic khởi tạo xong
     *   2. onBeginningOfSpeech  — VAD phát hiện tiếng nói
     *   3. onRmsChanged         — liên tục trong khi nói
     *   4. onPartialResults     — text trung gian mỗi 500ms
     *   5. onEndOfSpeech        — im lặng > SILENCE_TIMEOUT_MS
     *   6. onResults            — text cuối cùng
     *
     * Nếu không có giọng nói trong MAX_RECORD_MS:
     *   → onError(ERROR_SPEECH_TIMEOUT)
     *
     * @throws SecurityException nếu thiếu RECORD_AUDIO permission
     */
    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startListening() {
        if (!isLoaded) {
            onError?.invoke(
                SpeechRecognizer.ERROR_CLIENT,
                "Chưa gọi load() — model chưa được khởi tạo"
            )
            return
        }

        // ── Wire AudioProcessor callbacks ──────

        audioProcessor.onStateChange = { recordState ->
            when (recordState) {
                RecordingState.LISTENING  -> onReadyForSpeech?.invoke()
                RecordingState.SPEAKING   -> onBeginningOfSpeech?.invoke()
                RecordingState.PROCESSING -> onEndOfSpeech?.invoke()
                RecordingState.IDLE       -> Unit
            }
        }

        audioProcessor.onSpeechChunk = { chunk ->
            // RMS → dB (approximate, range 0–10 cho UI)
            val rmsFloat = chunk.pcmFloat.map { it * it }.average().toFloat()
            val rmsDb = (10f * Math.log10(rmsFloat.toDouble() + 1e-9f)).toFloat()
                .coerceIn(-40f, 0f)
                .let { (it + 40f) / 4f }  // normalize → [0, 10]
            onRmsChanged?.invoke(rmsDb)

            // Inference partial
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val (text, confidence) = inferWithConfidence(chunk)
                    if (text.isNotBlank() && confidence >= minPartialConfidence) {
                        withContext(Dispatchers.Main) {
                            onPartialResults?.invoke(text)
                        }
                    }
                }.onFailure { e ->
                    withContext(Dispatchers.Main) {
                        onError?.invoke(
                            SpeechRecognizer.ERROR_CLIENT,
                            "Lỗi nhận dạng trung gian: ${e.message}"
                        )
                    }
                }
            }
        }

        audioProcessor.onSpeechEnd = { chunk ->
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val (text, _) = inferWithConfidence(chunk)
                    withContext(Dispatchers.Main) {
                        if (text.isBlank()) {
                            onError?.invoke(
                                SpeechRecognizer.ERROR_NO_MATCH,
                                "Không nhận dạng được giọng nói"
                            )
                        } else {
                            onResults?.invoke(text)
                        }
                    }
                }.onFailure { e ->
                    withContext(Dispatchers.Main) {
                        onError?.invoke(
                            SpeechRecognizer.ERROR_CLIENT,
                            "Lỗi nhận dạng: ${e.message}"
                        )
                    }
                }
            }
        }

        audioProcessor.onError = { msg ->
            onError?.invoke(SpeechRecognizer.ERROR_AUDIO, msg)
        }

        audioProcessor.start()
    }

    /**
     * Dừng thu âm, kích hoạt nhận dạng với audio đã thu.
     * Tương đương [SpeechRecognizer.stopListening].
     */
    fun stopListening() {
        audioProcessor.stop()
    }

    /**
     * Hủy ngay lập tức, không phát onResults.
     * Tương đương [SpeechRecognizer.cancel].
     */
    fun cancel() {
        audioProcessor.stop()
    }

    /**
     * Giải phóng toàn bộ resources.
     * Tương đương [SpeechRecognizer.destroy].
     * Sau khi gọi destroy(), object này không dùng được nữa.
     */
    fun destroy() {
        close()
    }

    override fun close() {
        audioProcessor.stop()
        session?.close()
        ortEnv?.close()
        scope.cancel()
        isLoaded = false
    }

    // ── Core inference ────────────────────────

    /**
     * Chạy pipeline đầy đủ: pre-process → ONNX inference → CTC decode.
     *
     * Thứ tự xử lý:
     *   1. Pre-emphasis (α=0.97) — tăng cường tần số cao
     *   2. Trim silence — cắt khoảng im lặng đầu/cuối
     *   3. Guard — bỏ qua nếu audio < 0.1 giây
     *   4. ONNX inference — float32 → logits
     *   5. CTC greedy decode + confidence → (text, confidence)
     *
     * @return Pair(text, confidence) — confidence ∈ [0, 1]
     */
    private fun inferWithConfidence(chunk: AudioChunk): Pair<String, Float> {
        var audio = chunk.pcmFloat

        // Bước 1: Pre-emphasis
        audio = audioProcessor.preEmphasis(audio, alpha = 0.97f)

        // Bước 2: Trim silence
        audio = audioProcessor.trimSilence(audio, threshold = 0.01f)

        // Bước 3: Guard — wav2vec2 cần tối thiểu ~0.1 giây = 1600 samples
        if (audio.size < 1600) return "" to 0f

        // Bước 4: ONNX inference
        val logitsFlat = runInference(audio)

        // Bước 5: CTC decode
        return CTCTextDecoder.greedyDecodeWithConfidence(logitsFlat, vocabSize, tokens)
    }

    /**
     * Chuyển PCM float32 → text (không trả về confidence).
     * Dùng để gọi trực tiếp từ ngoài khi đã có FloatArray audio sẵn
     * (ví dụ: từ file WAV đã decode, không qua microphone).
     *
     * ```kotlin
     * val text = asr.transcribe(wavFloatArray)
     * ```
     *
     * @param audioFloat PCM float32 [-1, 1], 16 kHz, mono
     * @return văn bản nhận dạng, lowercase, đã trim — hoặc "" nếu thất bại
     */
    fun transcribe(audioFloat: FloatArray): String {
        if (!isLoaded) error("Chưa gọi load()")
        if (audioFloat.size < 1600) return ""

        val prepared = audioProcessor.run {
            val emphasized = preEmphasis(audioFloat, alpha = 0.97f)
            trimSilence(emphasized, threshold = 0.01f)
        }

        if (prepared.size < 1600) return ""
        val logits = runInference(prepared)
        return CTCTextDecoder.greedyDecode(logits, vocabSize, tokens)
    }

    /**
     * Chạy ONNX inference thuần túy.
     *
     * Input tensor: float32[1, num_samples] — tên input node: "input_values"
     * Output tensor: float32[1, time_steps, vocab_size] — tên output: "logits"
     *
     * @param audio normalized PCM float32 [-1, 1]
     * @return logits flat FloatArray, shape [time_steps × vocab_size]
     */
    private fun runInference(audio: FloatArray): FloatArray {
        val env     = ortEnv     ?: error("OrtEnvironment chưa khởi tạo")
        val sess    = session    ?: error("OrtSession chưa khởi tạo")

        val inputShape  = longArrayOf(1L, audio.size.toLong())
        val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(audio), inputShape)

        val results = sess.run(mapOf("input_values" to inputTensor))

        var logitsTensor: OnnxTensor? = null
        for (entry in results) {
            if (entry.key == "logits") {
                logitsTensor = entry.value as OnnxTensor
                break
            }
        }
        val tensor = logitsTensor ?: error("Output 'logits' không tìm thấy trong model output")

        val totalElements = tensor.info.shape.reduce { acc, l -> acc * l }.toInt()
        val flat = FloatArray(totalElements)
        tensor.floatBuffer.get(flat)
        return flat
    }

    // ── Companion ─────────────────────────────

    companion object {

        /**
         * Vocab tiếng Việt — dùng với model fine-tuned trên dữ liệu tiếng Việt.
         * Ví dụ: nguyenvulebinh/wav2vec2-base-vi-vlsp2020
         *
         * Truyền vào constructor: Wav2Vec2SpeechRecognizer(context, VOCAB_VI)
         */
        val VOCAB_VI: List<String> = listOf(
            "<pad>", "<s>", "</s>", "<unk>", "|",
            "a", "ă", "â", "b", "c", "d", "đ", "e", "ê", "g",
            "h", "i", "k", "l", "m", "n", "o", "ô", "ơ", "p",
            "q", "r", "s", "t", "u", "ư", "v", "x", "y"
        )

        /**
         * Kiểm tra nhanh xem thiết bị có đủ RAM để chạy model không.
         * Model quantize int8 ~110MB cần ~300MB RAM khi inference.
         *
         * @return true nếu RAM khả dụng > 400MB
         */
        fun isDeviceCapable(context: Context): Boolean {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE)
                    as android.app.ActivityManager
            val info = android.app.ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            return info.availMem > 400L * 1024 * 1024
        }
    }
}

/*
 * ─────────────────────────────────────────────────────────────────────────────
 * Hướng dẫn Export ONNX
 * (chạy trên máy tính, không phải Android)
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * ### English model (wav2vec2-base-960h)
 *
 * pip install transformers torch onnx onnxruntime
 *
 * from transformers import Wav2Vec2ForCTC
 * import torch
 *
 * MODEL_ID = "facebook/wav2vec2-base-960h"
 * model    = Wav2Vec2ForCTC.from_pretrained(MODEL_ID).eval()
 *
 * dummy = torch.zeros(1, 48000)   # 3 giây @ 16kHz
 *
 * torch.onnx.export(
 *     model, dummy, "wav2vec2_asr.onnx",
 *     input_names  = ["input_values"],
 *     output_names = ["logits"],
 *     dynamic_axes = {
 *         "input_values": {0: "batch", 1: "num_samples"},
 *         "logits":       {0: "batch", 1: "time_steps"},
 *     },
 *     opset_version = 14,
 * )
 *
 * # Quantize int8 → ~110MB (từ ~380MB)
 * from onnxruntime.quantization import quantize_dynamic, QuantType
 * quantize_dynamic(
 *     "wav2vec2_asr.onnx",
 *     "wav2vec2_asr.onnx",
 *     weight_type          = QuantType.QInt8,
 *     op_types_to_quantize = ["MatMul"],   # CHỈ MatMul — tránh lỗi ConvInteger
 * )
 *
 * ### Vietnamese model
 *
 * MODEL_ID = "nguyenvulebinh/wav2vec2-base-vi-vlsp2020"
 * # Thay MODEL_ID và dùng VOCAB_VI khi tạo Wav2Vec2SpeechRecognizer
 *
 * ### Đặt file vào project
 *
 * cp wav2vec2_asr.onnx  <project>/app/src/main/assets/
 *
 * ─────────────────────────────────────────────────────────────────────────────
 */
