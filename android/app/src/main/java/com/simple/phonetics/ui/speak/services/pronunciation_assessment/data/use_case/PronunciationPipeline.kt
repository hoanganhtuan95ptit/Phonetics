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
 *      │  Cắt chunks mỗi 500ms
 *      ▼
 *   Wav2Vec2Phoneme          ← OnnxRuntime Android
 *      │  float32[-1,1] → IPA phoneme list
 *      │  CTC decode: [h, ɛ, l, oʊ]
 *      ▼
 *   PronunciationScorer
 *      │  G2P(referenceText) → [h, ɛ, l, oʊ]  (chuẩn)
 *      │  alignPartial(ref, spoken)
 *      │  GOPScorer → điểm từng phoneme
 *      ▼
 *   SentenceScore            ← trả về UI
 *
 * Cách dùng:
 *   val pipeline = PronunciationPipeline(context)
 *   pipeline.prepare("hello world")     // load model + set reference
 *
 *   pipeline.onPartialResult = { score ->
 *       updateUI(score)                  // update realtime mỗi 500ms
 *   }
 *   pipeline.onFinalResult = { score ->
 *       showFinalScore(score)
 *   }
 *
 *   pipeline.startListening()           // bắt đầu thu âm
 *   // ... người dùng nói ...
 *   // pipeline tự dừng khi phát hiện im lặng
 *   // hoặc:
 *   pipeline.stopListening()            // dừng thủ công
 *
 *   pipeline.close()                    // giải phóng resources
 */

package com.simple.phonetics.ui.speak.services.pronunciation_assessment.data.use_case

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*

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
    /** Gọi mỗi 500ms trong khi người dùng đang nói */
    var onPartialResult: ((SentenceScore) -> Unit)? = null

    /** Gọi khi người dùng dừng nói — kết quả cuối */
    var onFinalResult: ((SentenceScore) -> Unit)? = null

    /** Gọi khi pipeline state thay đổi */
    var onStateChange: ((PipelineState) -> Unit)? = null

    /** Gọi khi có lỗi */
    var onError: ((String) -> Unit)? = null

    // ── Components ────────────────────────────
    private val audioProcessor   = AudioProcessor(context)
    private val phonemeRecognizer = Wav2Vec2Phoneme(context)
    private val scorer           = PronunciationScorer()
    private val scope            = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Reference dạng list cặp (word, IPA phonemes).
     * Ví dụ: [("the", ["ð","ə"]), ("cat", ["k","æ","t"])]
     *
     * Caller cấp sẵn IPA từ dictionary của họ → pipeline không phụ thuộc G2P nội bộ.
     */
    var referenceWords: List<Pair<String, List<String>>> = emptyList()
        private set

    /** Câu tham chiếu được derive từ referenceWords (join các word). */
    val referenceText: String
        get() = referenceWords.joinToString(" ") { it.first }

    var state = PipelineState.UNINITIALIZED
        private set(value) { field = value; onStateChange?.invoke(value) }

    // ── Lifecycle ─────────────────────────────

    /**
     * Load model và set reference.
     * Gọi 1 lần khi khởi tạo — blocking ~300ms (model load).
     *
     * @param reference  Danh sách cặp (word, IPA phonemes) của câu cần đọc.
     *                   Ví dụ: listOf("the" to listOf("ð","ə"),
     *                                  "cat" to listOf("k","æ","t"))
     * @param useGPU     Dùng NNAPI acceleration nếu có
     */
    suspend fun prepare(
        reference: List<Pair<String, List<String>>>,
        useGPU: Boolean = false
    ) {
        withContext(Dispatchers.IO) {
            try {
                phonemeRecognizer.load(useGPU = useGPU)
                referenceWords = reference
                withContext(Dispatchers.Main) { state = PipelineState.READY }
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

        // ── Wiring AudioProcessor callbacks ───
        audioProcessor.onStateChange = { recordState ->
            state = when (recordState) {
                RecordingState.LISTENING  -> PipelineState.LISTENING
                RecordingState.SPEAKING   -> PipelineState.RECORDING
                RecordingState.PROCESSING -> PipelineState.PROCESSING
                RecordingState.IDLE       -> PipelineState.READY
            }
        }

        audioProcessor.onSpeechChunk = { chunk ->
            // Chạy trong IO thread — wav2vec2 inference tốn ~50-150ms
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val phonemes = recognizePhonemes(chunk)
                    val score    = scorer.scorePartial(
                        wordPhonemes   = referenceWords,
                        spokenPhonemes = phonemes
                    )
                    withContext(Dispatchers.Main) {
                        onPartialResult?.invoke(score)
                    }
                }.onFailure { e ->
                    withContext(Dispatchers.Main) {
                        onError?.invoke("Lỗi partial: ${e.message}")
                    }
                }
            }
        }

        audioProcessor.onSpeechEnd = { chunk ->
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val phonemes = recognizePhonemes(chunk)
                    val score    = scorer.score(
                        wordPhonemes   = referenceWords,
                        spokenPhonemes = phonemes
                    )
                    withContext(Dispatchers.Main) {
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
            state = PipelineState.ERROR
            onError?.invoke(msg)
        }

        audioProcessor.start()
    }

    fun stopListening() {
        audioProcessor.stop()
        state = PipelineState.READY
    }

    // ── Inference ─────────────────────────────

    /**
     * Chạy toàn bộ audio processing + wav2vec2 inference.
     *
     * Thứ tự xử lý:
     *   1. Pre-emphasis filter — tăng cường tần số cao
     *   2. Trim silence — cắt bỏ khoảng lặng đầu/cuối
     *   3. Wav2Vec2 inference → IPA phonemes
     */
    private fun recognizePhonemes(chunk: AudioChunk): List<String> {
        var audio = chunk.pcmFloat

        // Bước 1: Pre-emphasis
        audio = audioProcessor.preEmphasis(audio, alpha = 0.97f)

        // Bước 2: Trim silence
        audio = audioProcessor.trimSilence(audio, threshold = 0.01f)

        // Bước 3: Guard — nếu audio quá ngắn sau trim, bỏ qua
        // wav2vec2 cần tối thiểu ~0.1 giây = 1600 samples
        if (audio.size < 1600) return emptyList()

        // Bước 4: Wav2Vec2 inference → IPA phonemes
        return phonemeRecognizer.recognize(audio)
    }

    override fun close() {
        audioProcessor.stop()
        phonemeRecognizer.close()
        scope.cancel()
    }
}
