/**
 * PronunciationPipeline.kt
 *
 * Class trung tâm — nối AudioProcessor + Wav2Vec2Phoneme + PronunciationScorer
 * thành một pipeline hoàn chỉnh.
 *
 * Luồng dữ liệu đầy đủ:
 *
 *   Microphone
 *      │  PCM 16-bit, 16kHz
 *      ▼
 *   AudioProcessor
 *      │  VAD phát hiện giọng nói
 *      │  Cắt chunks mỗi 500ms (sliding accumulation)
 *      ▼
 *   PronunciationPipeline.normalize
 *      │  zero-mean / unit-variance (chuẩn input của Wav2Vec2)
 *      ▼
 *   Wav2Vec2Phoneme          ← OnnxRuntime Android
 *      │  float32 → IPA phoneme list
 *      │  CTC decode dựa vocab thật (392 token)
 *      ▼
 *   PronunciationScorer
 *      │  alignPartial(ref, spoken)
 *      │  GOPScorer → điểm từng phoneme
 *      ▼
 *   SentenceScore            ← trả về UI
 *
 * Lưu ý quan trọng:
 *   - KHÔNG pre-emphasis trước Wav2Vec2. Model end-to-end này
 *     được train trên raw waveform đã normalize, pre-emphasis sẽ
 *     làm méo phân bố input và phá CTC logits.
 *   - KHÔNG trimSilence trên partial chunk — VAD đã handle. Trim
 *     ở partial sẽ làm reference phoneme bị "trượt" theo thời gian.
 *
 * Cách dùng:
 *   val pipeline = PronunciationPipeline(context)
 *   pipeline.prepare(reference)         // load model + set reference
 *
 *   pipeline.onPartialResult = { score -> updateUI(score) }
 *   pipeline.onFinalResult   = { score -> showFinalScore(score) }
 *
 *   pipeline.startListening()
 *   // ... pipeline tự dừng khi phát hiện im lặng
 *   pipeline.close()
 */

package com.simple.phonetics.ui.speak.services.pronunciation_assessment.data.use_case

import android.content.Context
import android.util.Log
import com.simple.core.utils.extentions.toJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

// ─────────────────────────────────────────────
// Pipeline state
// ─────────────────────────────────────────────

enum class PipelineState {
    UNINITIALIZED,  // chưa load model
    READY,          // sẵn sàng
    LISTENING,      // đang chờ giọng nói
    RECORDING,      // đang ghi âm
    PROCESSING,     // đang chạy wav2vec2 + scorer
    ERROR
}

// ─────────────────────────────────────────────
// PronunciationPipeline
// ─────────────────────────────────────────────

class PronunciationPipeline(private val context: Context) : AutoCloseable {

    // ── Callbacks ─────────────────────────────
    /** Gọi mỗi ~500ms trong khi người dùng đang nói */
    var onPartialResult: ((SentenceScore) -> Unit)? = null

    /** Gọi khi người dùng dừng nói — kết quả cuối */
    var onRecordEnd: (() -> Unit)? = null

    /** Gọi khi người dùng dừng nói — kết quả cuối */
    var onFinalResult: ((SentenceScore) -> Unit)? = null

    /** Gọi khi pipeline state thay đổi */
    var onStateChange: ((PipelineState) -> Unit)? = null

    /** Gọi khi có lỗi */
    var onError: ((String) -> Unit)? = null

    // ── Components ────────────────────────────
    private val audioProcessor    = AudioProcessor(context)
    private val phonemeRecognizer = Wav2Vec2Phoneme(context)
    private val scorer            = PronunciationScorer()
    private val scope             = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Channel CONFLATED — chỉ giữ chunk mới nhất. Nếu inference cũ
     * chưa xong mà chunk mới đến, chunk cũ bị thay → không có hiện tượng
     * partial cũ về sau partial mới khiến UI nhảy lùi.
     */
    private var partialChannel: Channel<AudioChunk>? = null
    private var partialWorker:  Job? = null

    /**
     * Reference dạng list cặp (word, IPA phonemes).
     * Ví dụ: [("the", ["ð","ə"]), ("cat", ["k","æ","t"])]
     */
    var referenceWords: List<Pair<String, List<String>>> = emptyList()
        private set

    /**
     * Dictionary grapheme–phoneme — load lazy lần đầu dùng.
     * Pipeline tự động điền [PhonemeScore.grapheme] cho mọi kết quả partial và final.
     */
    private val phonemeDict: PhonemeDict by lazy { PhonemeDict.load(context) }

    val referenceText: String
        get() = referenceWords.joinToString(" ") { it.first }

    var state = PipelineState.UNINITIALIZED
        private set(value) { field = value; onStateChange?.invoke(value) }

    // ── Lifecycle ─────────────────────────────

    /**
     * Load model và set reference.
     * Gọi 1 lần khi khởi tạo — blocking ~300ms (model load).
     */
    suspend fun prepare(
        reference: List<Pair<String, List<String>>>,
        useGPU: Boolean = false,
        onProgress: ((percent: Int) -> Unit)? = null,
    ) {
        withContext(Dispatchers.IO) {
            try {
                phonemeRecognizer.load(useGPU = useGPU, onProgress = onProgress)
                withContext(Dispatchers.Main) {
                    referenceWords = reference
                    state = PipelineState.READY
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    state = PipelineState.ERROR
                    Log.d("tuanha", "prepare: ", e)
                    onError?.invoke("Không load được model: ${e.message}")
                }
            }
        }
    }

    /** Đổi reference mà không cần load lại model */
    fun setReference(reference: List<Pair<String, List<String>>>) {
        referenceWords = reference
    }

    // ── Recording ─────────────────────────────

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startListening() {
        if (state == PipelineState.UNINITIALIZED) {
            onError?.invoke("Chưa gọi prepare()")
            return
        }
        if (referenceWords.isEmpty()) {
            onError?.invoke("Chưa set câu tham chiếu")
            return
        }

        // ── Khởi tạo partial worker (conflated channel) ────────
        // Chỉ 1 inference partial chạy tại 1 thời điểm, chunk đến trong khi
        // worker bận sẽ overwrite chunk cũ → không pile-up.
        partialWorker?.cancel()
        partialChannel?.close()
        val ch = Channel<AudioChunk>(Channel.CONFLATED)
        partialChannel = ch
        partialWorker = scope.launch(Dispatchers.IO) {
            for (chunk in ch) {
                runCatching {
                    Log.d("tuanha", "startListening: partialWorker")
                    val phonemes = recognizePhonemes(chunk)
                    val score = scorer.scorePartial(
                        wordPhonemes   = referenceWords,
                        spokenPhonemes = phonemes,
                        fluencyPenalty = chunk.pauseCount.toFluencyPenalty(),
                        phonemeDict    = phonemeDict   // populated by lazy internal dict
                    )
                    withContext(Dispatchers.Main) {
                        Log.d("tuanha", "startListening: partialWorker end")
                        onPartialResult?.invoke(score)
                    }
                }.onFailure { e ->
                    withContext(Dispatchers.Main) {
                        onError?.invoke("Lỗi partial: ${e.message}")
                    }
                }
            }
        }

        // ── Wiring AudioProcessor callbacks ───
        audioProcessor.onStateChange = { recordState ->
            Log.d("tuanha", "startListening: onStateChange:$recordState")
            state = when (recordState) {
                RecordingState.LISTENING  -> PipelineState.LISTENING
                RecordingState.SPEAKING   -> PipelineState.RECORDING
                RecordingState.PROCESSING -> PipelineState.PROCESSING
                RecordingState.IDLE       -> PipelineState.READY
            }
        }

        audioProcessor.onSpeechChunk = { chunk ->
            Log.d("tuanha", "startListening: onSpeechChunk")
            // Đẩy vào channel — nếu worker bận, chunk cũ sẽ bị overwrite
            partialChannel?.trySend(chunk)
        }

        audioProcessor.onSpeechEnd = { chunk ->
            // Đóng channel partial — không chạy partial nữa, dồn lực cho final
            partialWorker?.cancel()
            partialChannel?.close()
            Log.d("tuanha", "startListening: onSpeechEnd")
            scope.launch(Dispatchers.IO) {
                runCatching {
                    onRecordEnd?.invoke()
                    val phonemes = recognizePhonemes(chunk)
                    val score = scorer.score(
                        wordPhonemes   = referenceWords,
                        spokenPhonemes = phonemes,
                        fluencyPenalty = chunk.pauseCount.toFluencyPenalty(),
                        phonemeDict    = phonemeDict
                    ).copy(audioFilePath = chunk.audioFilePath)

                    withContext(Dispatchers.Main) {
                        Log.d("tuanha", "startListening: onSpeechEnd end ----:${score.toJson()}")
                        onFinalResult?.invoke(score)
                        state = PipelineState.READY
                    }
                }.onFailure { e ->
                    withContext(Dispatchers.Main) {
                        state = PipelineState.ERROR
                        onError?.invoke("Lỗi final: ${e.message}")
                    }
                }
            }
        }

        audioProcessor.onError = { msg ->
            Log.d("tuanha", "startListening: onError")
            state = PipelineState.ERROR
            onError?.invoke(msg)
        }

        audioProcessor.start()
    }

    fun stopListening() {
        audioProcessor.stop()
        partialChannel?.close()
        partialChannel = null
        partialWorker?.cancel()
        partialWorker = null
        state = PipelineState.READY
    }

    // ── Inference ─────────────────────────────

    /**
     * Chạy Wav2Vec2 inference trên audio chunk.
     *
     * Pre-processing:
     *   1. Guard — bỏ qua nếu audio quá ngắn (< 0.1s = 1600 samples)
     *   2. Normalize zero-mean / unit-variance — chuẩn input của
     *      Wav2Vec2FeatureExtractor (HuggingFace). KHÔNG pre-emphasis.
     *
     * Tại sao không trimSilence trước khi inference?
     *   VAD trong AudioProcessor đã tách speech khỏi silence. Trim
     *   thêm 1 lần nữa trên partial chunk = làm chunk ngắn hơn từng
     *   partial, model thấy audio khác nhau → CTC alignment dao động.
     */
    private fun recognizePhonemes(chunk: AudioChunk): List<String> {
        val raw = chunk.pcmFloat

        if (raw.size < 1600) return emptyList()

        // Normalize zero-mean / unit-variance
        val audio = normalize(raw)

        return phonemeRecognizer.recognize(audio)
    }

    /**
     * Normalize tín hiệu về zero-mean / unit-variance.
     * Đây là step chuẩn của Wav2Vec2FeatureExtractor (do_normalize=True).
     * Đảm bảo phân bố input giống lúc train → CTC logits ổn định.
     */
    private fun normalize(signal: FloatArray): FloatArray {
        if (signal.isEmpty()) return signal

        // Mean
        var sum = 0.0
        for (v in signal) sum += v
        val mean = (sum / signal.size).toFloat()

        // Std
        var sqSum = 0.0
        for (v in signal) {
            val d = v - mean
            sqSum += d * d
        }
        val std = sqrt(sqSum / signal.size).toFloat() + 1e-7f

        val invStd = 1f / std
        return FloatArray(signal.size) { (signal[it] - mean) * invStd }
    }

    override fun close() {
        audioProcessor.stop()
        partialChannel?.close()
        partialChannel = null
        partialWorker?.cancel()
        partialWorker = null
        phonemeRecognizer.close()
        scope.cancel()
    }
}

/**
 * Chuyển số lần dừng giữa câu thành điểm trừ fluency (0–20).
 *   0 lần dừng → 0 điểm trừ
 *   1 lần       → 5
 *   2 lần       → 10
 *   3 lần       → 15
 *   ≥4 lần      → 20 (tối đa)
 */
private fun Int.toFluencyPenalty(): Int = (this * 5).coerceIn(0, 20)
