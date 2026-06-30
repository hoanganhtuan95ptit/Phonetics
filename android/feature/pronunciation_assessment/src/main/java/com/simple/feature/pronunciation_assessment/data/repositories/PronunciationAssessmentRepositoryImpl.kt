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
 *      │  float32 -> IPA phoneme list
 *      │  CTC decode dựa vocab thật (392 token)
 *      ▼
 *   PronunciationScorer
 *      │  align(ref, spoken)
 *      │  GOPScorer -> điểm từng phoneme
 *      ▼
 *   AssessmentEvent.RecordEnded / Final / Error  ← phát qua Flow
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
import com.simple.feature.pronunciation_assessment.BuildConfig
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
import com.simple.phonetics.entities.SentenceScore
import com.simple.state.ResultState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class PronunciationAssessmentRepositoryImpl(
    private val audioRecorder: AudioRecorder,
    private val phonemeRecognizer: PhonemeRecognizer,
    private val phonemeDictionary: PhonemeDictionary,
) : PronunciationAssessmentRepository {

    @Volatile
    private var isModelReady = false

    // ── Lifecycle ─────────────────────────────

    override fun prepare(useGPU: Boolean): Flow<ResultState<Int>> = channelFlow {

        try {

            phonemeRecognizer.load(useGPU = useGPU, onProgress = { percent ->

                trySend(ResultState.Running(percent.coerceIn(MIN_PROGRESS, MAX_PROGRESS)))
            })

            isModelReady = true
            trySend(ResultState.Success(MAX_PROGRESS))
        } catch (e: Exception) {

            isModelReady = false
            trySend(ResultState.Failed(e))
        }
    }

    // ── Recording ─────────────────────────────

    /**
     * Mỗi lần gọi [start] là một phiên record độc lập.
     * Reference được truyền theo phiên để repository không giữ stale reference giữa các lần chấm.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun start(referenceWords: List<Pair<String, List<String>>>): Flow<AssessmentEvent> = channelFlow {

        if (!validateStart(referenceWords)){
            return@channelFlow
        }

        bindRecorderCallbacks(referenceWords)
        audioRecorder.start()

        awaitClose {

            // Flow có thể đóng do final, lỗi, hoặc collector cancel. Luôn dừng mic ở một chỗ.
            audioRecorder.stop()
        }
    }

    /**
     * Guard nhẹ trước khi mở mic. Model phải được prepare, còn reference là input của phiên record.
     */
    private fun ProducerScope<AssessmentEvent>.validateStart(referenceWords: List<Pair<String, List<String>>>, ): Boolean {

        if (!isModelReady) {

            trySend(AssessmentEvent.Error("Chưa gọi prepare()"))
            return false
        }

        if (referenceWords.isEmpty()) {

            trySend(AssessmentEvent.Error("Chưa set câu tham chiếu"))
            return false
        }

        return true
    }

    /**
     * AudioRecorder vẫn là callback API, repository wrap callback đó thành Flow event cho domain/UI.
     */
    private fun ProducerScope<AssessmentEvent>.bindRecorderCallbacks(referenceWords: List<Pair<String, List<String>>>, ) {

        val eventScope = this

        audioRecorder.onStateChange = { recordState ->

            emitRecorderState(recordState)
        }

        audioRecorder.onSpeechChunk = {

            // Partial chunk được AudioRecorder phát mỗi ~500 ms.
            // UI hiện chưa consume Partial nên repository chỉ chấm final để tránh inference realtime thừa.
        }

        audioRecorder.onSpeechEnd = { chunk ->

            launch(Dispatchers.IO) {

                assessFinalChunk(referenceWords, chunk)
            }
        }

        audioRecorder.onError = { message ->

            trySend(AssessmentEvent.Error(message))
            eventScope.close()
        }
    }

    private fun ProducerScope<AssessmentEvent>.emitRecorderState(recordState: RecordingState) {

        val assessmentState = recordState.toAssessmentState()

        if (assessmentState == AssessmentState.ERROR) {

            trySend(AssessmentEvent.Error("Lỗi"))
            return
        }

        trySend(AssessmentEvent.StateChanged(assessmentState))
    }

    private fun RecordingState.toAssessmentState(): AssessmentState {

        return when (this) {

            RecordingState.LISTENING -> AssessmentState.LISTENING
            RecordingState.SPEAKING -> AssessmentState.RECORDING
            RecordingState.PROCESSING -> AssessmentState.PROCESSING
            RecordingState.IDLE -> AssessmentState.READY
            else -> AssessmentState.ERROR
        }
    }

    /**
     * Final chunk là audio đã gom đủ câu nói. Chỉ lúc này mới chạy model + scorer đầy đủ.
     */
    private suspend fun ProducerScope<AssessmentEvent>.assessFinalChunk(
        referenceWords: List<Pair<String, List<String>>>,
        chunk: AudioChunk,
    ) {

        runCatching {

            trySend(AssessmentEvent.RecordEnded)

            val result = recognize(chunk)
            val score = scoreFinal(referenceWords, chunk, result)

            debugFinalResult(referenceWords, result, score)
            trySend(AssessmentEvent.Final(score))
        }.onFailure { e ->

            trySend(AssessmentEvent.Error("Lỗi final: ${e.message}"))
        }

        this.close()
    }

    private fun scoreFinal(
        referenceWords: List<Pair<String, List<String>>>,
        chunk: AudioChunk,
        result: RecognitionResult,
    ): SentenceScore {

        return PronunciationScorer.score(
            wordPhonemes = referenceWords,
            spokenPhonemes = result.phonemes,
            spokenFrames = result.frames,
            fluencyPenalty = chunk.pauseCount.toFluencyPenalty(),
            phonemeDict = phonemeDictionary,
        ).copy(audioFilePath = chunk.audioFilePath)
    }

    private fun debugFinalResult(
        referenceWords: List<Pair<String, List<String>>>,
        result: RecognitionResult,
        score: SentenceScore,
    ) {

        if (!BuildConfig.DEBUG) return

        Log.d(LOG_TAG, "phonemes: ${result.phonemes}")

        val framesLog = result.frames.joinToString { frame ->

            "${frame.phoneme}@${frame.durationMs}ms"
        }

        Log.d(LOG_TAG, "frames: $framesLog")
        Log.d(LOG_TAG, "referenceWords: ${referenceWords.toJson()}")
        Log.d(LOG_TAG, "score: ${score.toJson()}")
    }

    // ── Inference ─────────────────────────────

    /**
     * Chạy Wav2Vec2 inference trên audio chunk + lấy frame timing cho length scoring.
     *
     * Pre-processing:
     *   1. Guard — bỏ qua nếu audio quá ngắn (< 0.1s = 1600 samples)
     *   2. Normalize zero-mean / unit-variance — chuẩn input của Wav2Vec2FeatureExtractor.
     */
    private fun recognize(chunk: AudioChunk): RecognitionResult {

        val raw = chunk.pcmFloat
        if (raw.size < MIN_AUDIO_SAMPLE_COUNT) return RecognitionResult(emptyList(), emptyList())

        val audio = normalize(raw)
        return phonemeRecognizer.recognizeWithTiming(audio)
    }

    /**
     * Normalize tín hiệu về zero-mean / unit-variance giống Wav2Vec2FeatureExtractor.
     */
    private fun normalize(signal: FloatArray): FloatArray {

        if (signal.isEmpty()) return signal

        val mean = signal.average().toFloat()
        val variance = signal.sumOf { sample ->

            val delta = sample - mean
            (delta * delta).toDouble()
        } / signal.size

        val invStd = 1f / (sqrt(variance).toFloat() + EPSILON)
        return FloatArray(signal.size) { index ->

            (signal[index] - mean) * invStd
        }
    }

    // ── Release ───────────────────────────────

    /**
     * AutoCloseable contract: đóng toàn bộ tài nguyên dài hạn của pipeline.
     * Kết thúc một phiên record thì ProducerScope.close + awaitClose đã dừng mic, không unload model ở đó.
     */
    override fun close() {

        release()
    }

    override fun release() {

        audioRecorder.stop()
        phonemeRecognizer.close()
        isModelReady = false
    }

    companion object {

        private const val LOG_TAG = "tuanha"
        private const val MIN_PROGRESS = 0
        private const val MAX_PROGRESS = 100
        private const val MIN_AUDIO_SAMPLE_COUNT = 1600
        private const val EPSILON = 1e-7f
    }
}

/**
 * Chuyển số lần dừng giữa câu thành điểm trừ fluency (0-20).
 *   0 lần dừng -> 0 điểm trừ
 *   1 lần      -> 5
 *   >=4 lần    -> 20 (tối đa)
 */
private fun Int.toFluencyPenalty(): Int = (this * 5).coerceIn(0, 20)
