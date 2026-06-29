/**
 * PronunciationAssessmentRepositoryImpl.kt
 *
 * Orchestrator — nối [AudioRecorder] + [PhonemeRecognizer] + [PronunciationScorer]
 * thành một pipeline hoàn chỉnh.
 *
 * Luồng dữ liệu:
 *
 *   Microphone
 *      │  PCM 16-bit, 16 kHz
 *      ▼
 *   AudioRecorder
 *      │  VAD phát hiện giọng nói
 *      │  Cắt chunks mỗi 500 ms (sliding accumulation)
 *      ▼
 *   normalize()
 *      │  zero-mean / unit-variance (chuẩn input của Wav2Vec2)
 *      ▼
 *   PhonemeRecognizer (OnnxRuntime)
 *      │  float32 → IPA phoneme list
 *      │  CTC decode dựa vocab thật (392 token)
 *      ▼
 *   PronunciationScorer
 *      │  alignPartial(ref, spoken)
 *      │  GOPScorer → điểm từng phoneme
 *      ▼
 *   AssessmentEvent.Partial / Final  ← phát qua Flow
 *
 * Lưu ý quan trọng:
 *   - KHÔNG pre-emphasis trước Wav2Vec2 (model end-to-end train trên raw waveform).
 *   - KHÔNG trimSilence trên partial chunk — VAD đã handle.
 */

package com.simple.feature.pronunciation_assessment.data.repositories

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import com.simple.core.utils.extentions.toJson
import com.simple.feature.pronunciation_assessment.domain.entities.AssessmentEvent
import com.simple.feature.pronunciation_assessment.domain.entities.AssessmentState
import com.simple.feature.pronunciation_assessment.domain.entities.AudioChunk
import com.simple.feature.pronunciation_assessment.domain.entities.RecognitionResult
import com.simple.feature.pronunciation_assessment.domain.entities.RecordingState
import com.simple.feature.pronunciation_assessment.domain.repositories.AudioRecorder
import com.simple.feature.pronunciation_assessment.domain.repositories.PhonemeDictionary
import com.simple.feature.pronunciation_assessment.domain.repositories.PhonemeRecognizer
import com.simple.feature.pronunciation_assessment.domain.repositories.PronunciationAssessmentRepository
import com.simple.feature.pronunciation_assessment.domain.scoring.PronunciationScorer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class PronunciationAssessmentRepositoryImpl(
    private val audioRecorder: AudioRecorder,
    private val phonemeRecognizer: PhonemeRecognizer,
    private val phonemeDictionary: PhonemeDictionary,
) : PronunciationAssessmentRepository {

    // ── Internal components ───────────────────
    private val scorer = PronunciationScorer()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Channel CONFLATED — chỉ giữ chunk mới nhất. Nếu inference cũ chưa
     * xong mà chunk mới đến, chunk cũ bị thay → không có hiện tượng
     * partial cũ về sau partial mới khiến UI nhảy lùi.
     */
    private var partialChannel: Channel<AudioChunk>? = null
    private var partialWorker: Job? = null

    private val _state = MutableStateFlow(AssessmentState.UNINITIALIZED)
    override val state: StateFlow<AssessmentState> = _state.asStateFlow()

    override var referenceWords: List<Pair<String, List<String>>> = emptyList()
        private set

    override val referenceText: String
        get() = referenceWords.joinToString(" ") { it.first }

    // ── Lifecycle ─────────────────────────────

    override suspend fun prepare(
        reference: List<Pair<String, List<String>>>,
        useGPU: Boolean,
        onProgress: ((percent: Int) -> Unit)?,
    ) {
        withContext(Dispatchers.IO) {
            try {
                phonemeRecognizer.load(useGPU = useGPU, onProgress = onProgress)
                withContext(Dispatchers.Main) {
                    referenceWords = reference
                    _state.value = AssessmentState.READY
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.value = AssessmentState.ERROR
                    throw e
                }
            }
        }
    }

    override fun setReference(reference: List<Pair<String, List<String>>>) {
        referenceWords = reference
    }

    // ── Recording ─────────────────────────────

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun start(): Flow<AssessmentEvent> = channelFlow {

        if (_state.value == AssessmentState.UNINITIALIZED) {
            trySend(AssessmentEvent.Error("Chưa gọi prepare()"))
            return@channelFlow
        }
        if (referenceWords.isEmpty()) {
            trySend(AssessmentEvent.Error("Chưa set câu tham chiếu"))
            return@channelFlow
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
                    val result = recognize(chunk)
                    val score = scorer.scorePartial(
                        wordPhonemes = referenceWords,
                        spokenPhonemes = result.phonemes,
                        spokenFrames = result.frames,
                        fluencyPenalty = chunk.pauseCount.toFluencyPenalty(),
                        phonemeDict = phonemeDictionary,
                    )
                    trySend(AssessmentEvent.Partial(score))
                }.onFailure { e ->
                    trySend(AssessmentEvent.Error("Lỗi partial: ${e.message}"))
                }
            }
        }

        // ── Wire AudioRecorder callbacks ──────
        audioRecorder.onStateChange = { recordState ->

            val newState = when (recordState) {
                RecordingState.LISTENING -> AssessmentState.LISTENING
                RecordingState.SPEAKING -> AssessmentState.RECORDING
                RecordingState.PROCESSING -> AssessmentState.PROCESSING
                RecordingState.IDLE -> AssessmentState.READY
                else -> AssessmentState.ERROR
            }

            _state.value = newState

            if (newState == AssessmentState.ERROR) {
                trySend(AssessmentEvent.Error("Lỗi"))
            } else {
                trySend(AssessmentEvent.StateChanged(newState))
            }
        }

        audioRecorder.onSpeechChunk = { chunk ->
            // Đẩy vào channel — nếu worker bận, chunk cũ sẽ bị overwrite
            partialChannel?.trySend(chunk)
        }

        audioRecorder.onSpeechEnd = { chunk ->
            // Đóng channel partial — không chạy partial nữa, dồn lực cho final
            partialWorker?.cancel()
            partialChannel?.close()

            scope.launch(Dispatchers.IO) {
                runCatching {
                    trySend(AssessmentEvent.RecordEnded)
                    val result = recognize(chunk)
                    val score = scorer.score(
                        wordPhonemes = referenceWords,
                        spokenPhonemes = result.phonemes,
                        spokenFrames = result.frames,
                        fluencyPenalty = chunk.pauseCount.toFluencyPenalty(),
                        phonemeDict = phonemeDictionary,
                    ).copy(audioFilePath = chunk.audioFilePath)

                    Log.d("tuanha", "phonemes: ${result.phonemes}")
                    Log.d("tuanha", "frames: ${result.frames.joinToString { "${it.phoneme}@${it.durationMs}ms" }}")
                    Log.d("tuanha", "referenceWords: ${referenceWords.toJson()}")
                    Log.d("tuanha", "score: ${score.toJson()}")
                    trySend(AssessmentEvent.Final(score))
                    _state.value = AssessmentState.READY
                    close()
                }.onFailure { e ->
                    _state.value = AssessmentState.ERROR
                    trySend(AssessmentEvent.Error("Lỗi final: ${e.message}"))
                    close()
                }
            }
        }

        audioRecorder.onError = { msg ->
            _state.value = AssessmentState.ERROR
            trySend(AssessmentEvent.Error(msg))
            close()
        }

        audioRecorder.start()

        awaitClose {
            audioRecorder.stop()
            partialChannel?.close()
            partialChannel = null
            partialWorker?.cancel()
            partialWorker = null
        }
    }

    override fun stop() {
        audioRecorder.stop()
        partialChannel?.close()
        partialChannel = null
        partialWorker?.cancel()
        partialWorker = null
        _state.value = AssessmentState.READY
    }

    // ── Inference ─────────────────────────────

    /**
     * Chạy Wav2Vec2 inference trên audio chunk + lấy frame timing cho
     * length scoring.
     *
     * Pre-processing:
     *   1. Guard — bỏ qua nếu audio quá ngắn (< 0.1s = 1600 samples)
     *   2. Normalize zero-mean / unit-variance — chuẩn input của
     *      Wav2Vec2FeatureExtractor (HuggingFace). KHÔNG pre-emphasis.
     */
    private fun recognize(chunk: AudioChunk): RecognitionResult {
        val raw = chunk.pcmFloat
        if (raw.size < 1600) return RecognitionResult(emptyList(), emptyList())
        val audio = normalize(raw)
        return phonemeRecognizer.recognizeWithTiming(audio)
    }

    /**
     * Normalize tín hiệu về zero-mean / unit-variance.
     *
     * Đây là step chuẩn của Wav2Vec2FeatureExtractor (do_normalize=true).
     * Đảm bảo phân bố input giống lúc train → CTC logits ổn định.
     */
    private fun normalize(signal: FloatArray): FloatArray {
        if (signal.isEmpty()) return signal

        var sum = 0.0
        for (v in signal) sum += v
        val mean = (sum / signal.size).toFloat()

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
        audioRecorder.stop()
        partialChannel?.close()
        partialChannel = null
        partialWorker?.cancel()
        partialWorker = null
        scope.cancel()
    }

    override fun release() {
        phonemeRecognizer.close()
    }
}

/**
 * Chuyển số lần dừng giữa câu thành điểm trừ fluency (0–20).
 *   0 lần dừng → 0 điểm trừ
 *   1 lần       → 5
 *   ≥4 lần     → 20 (tối đa)
 */
private fun Int.toFluencyPenalty(): Int = (this * 5).coerceIn(0, 20)
