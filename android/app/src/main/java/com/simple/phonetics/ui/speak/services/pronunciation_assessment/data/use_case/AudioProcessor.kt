/**
 * AudioProcessor.kt
 *
 * Thu âm từ microphone Android, phát hiện giọng nói (VAD),
 * và cắt audio thành chunks để đưa vào wav2vec2.
 *
 * Luồng dữ liệu:
 *   AudioRecord (PCM 16-bit, 16kHz) 
 *     → VAD (energy-based)
 *       → onSpeechChunk(floatArray)  — realtime mỗi 500ms
 *       → onSpeechEnd(floatArray)    — khi người dùng dừng nói
 *
 * Cách dùng:
 *   val processor = AudioProcessor(context)
 *   processor.onSpeechChunk = { pcm -> scorer.scorePartial(pcm) }
 *   processor.onSpeechEnd   = { pcm -> scorer.scoreFinal(pcm) }
 *   processor.start()
 *   // ... người dùng nói ...
 *   processor.stop()
 *
 * Thư viện cần thêm vào build.gradle:
 *   implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.26.0'
 *   (cho Silero VAD ở phần nâng cao)
 */

package com.simple.phonetics.ui.speak.services.pronunciation_assessment.data.use_case

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext
import kotlin.math.sqrt

// ─────────────────────────────────────────────
// Constants
// ─────────────────────────────────────────────

private const val SAMPLE_RATE       = 16_000   // Hz — wav2vec2 yêu cầu 16kHz
private const val CHANNEL_CONFIG    = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_FORMAT      = AudioFormat.ENCODING_PCM_16BIT

// VAD parameters
private const val FRAME_SIZE_MS     = 20        // ms mỗi frame VAD
private const val FRAME_SAMPLES     = SAMPLE_RATE * FRAME_SIZE_MS / 1000  // = 320 samples
private const val ENERGY_THRESHOLD  = 500.0f    // RMS threshold — điều chỉnh theo môi trường
private const val SILENCE_TIMEOUT_MS = 800      // ms im lặng → coi là kết thúc câu
private const val MIN_SPEECH_MS     = 200       // ms tối thiểu để tính là có giọng nói
private const val MAX_RECORD_MS     = 15_000    // ms tối đa 1 lần ghi

// Partial scoring interval
private const val PARTIAL_INTERVAL_MS = 500     // ms — cứ 500ms chấm điểm 1 lần

// ─────────────────────────────────────────────
// Data
// ─────────────────────────────────────────────

enum class RecordingState {
    IDLE,       // chưa bắt đầu
    LISTENING,  // đang thu âm, chờ giọng nói
    SPEAKING,   // phát hiện giọng nói
    PROCESSING  // đã dừng, đang xử lý
}

data class AudioChunk(
    val pcmFloat: FloatArray,    // normalized [-1, 1] cho wav2vec2
    val durationMs: Int,
    val isFinal: Boolean         // true = người dùng đã dừng nói
)

// ─────────────────────────────────────────────
// AudioProcessor
// ─────────────────────────────────────────────

class AudioProcessor(private val context: Context) {

    // ── Callbacks ─────────────────────────────
    /** Gọi mỗi PARTIAL_INTERVAL_MS khi người dùng đang nói */
    var onSpeechChunk: ((AudioChunk) -> Unit)? = null

    /** Gọi khi người dùng dừng nói (im lặng > SILENCE_TIMEOUT_MS) */
    var onSpeechEnd: ((AudioChunk) -> Unit)? = null

    /** Gọi khi state thay đổi — để cập nhật UI */
    var onStateChange: ((RecordingState) -> Unit)? = null

    /** Gọi khi có lỗi */
    var onError: ((String) -> Unit)? = null

    // ── Internal state ────────────────────────
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Tất cả callback luôn được invoke trên main thread — recordLoop chạy ở
    // Dispatchers.IO mà callbacks thường đụng UI (setActivated, setText...) →
    // crash "Animators may only be run on Looper threads" nếu không marshal.
    private val mainHandler = Handler(Looper.getMainLooper())
    private fun <T> ((T) -> Unit)?.invokeOnMain(value: T) {
        val cb = this ?: return
        if (Looper.myLooper() == Looper.getMainLooper()) cb(value)
        else mainHandler.post { cb(value) }
    }

    private val speechBuffer = mutableListOf<Short>()   // toàn bộ audio đã nói
    private var state = RecordingState.IDLE
        set(value) { field = value; onStateChange.invokeOnMain(value) }

    // ── Start / Stop ──────────────────────────

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        if (state != RecordingState.IDLE) return

        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufSize = maxOf(minBuf * 2, FRAME_SAMPLES * 2 * 4)  // đủ cho vài frame

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufSize
        ).also { ar ->
            if (ar.state != AudioRecord.STATE_INITIALIZED) {
                onError.invokeOnMain("AudioRecord khởi tạo thất bại")
                return
            }
            ar.startRecording()
        }

        state = RecordingState.LISTENING
        speechBuffer.clear()
        recordingJob = scope.launch { recordLoop() }
    }

    fun stop() {
        recordingJob?.cancel()
        audioRecord?.apply { stop(); release() }
        audioRecord = null
        state = RecordingState.IDLE
        speechBuffer.clear()
    }

    // ── Core recording loop ───────────────────

    private suspend fun recordLoop() {
        val frameBuffer    = ShortArray(FRAME_SAMPLES)
        var silenceDuration = 0
        var speechDuration  = 0
        var totalDuration   = 0
        var lastPartialMs   = 0

        while (coroutineContext.isActive) {
            val read = audioRecord?.read(frameBuffer, 0, FRAME_SAMPLES) ?: break
            if (read <= 0) continue

            val rms      = computeRMS(frameBuffer, read)
            val isSpeech = rms > ENERGY_THRESHOLD
            val frameDuration = read * 1000 / SAMPLE_RATE

            totalDuration += frameDuration

            when {
                // ── Đang nghe, phát hiện tiếng nói ──────
                state == RecordingState.LISTENING && isSpeech -> {
                    state = RecordingState.SPEAKING
                    speechBuffer.addAll(frameBuffer.take(read))
                    speechDuration = frameDuration
                    silenceDuration = 0
                }

                // ── Đang nói, vẫn có tiếng ──────────────
                state == RecordingState.SPEAKING && isSpeech -> {
                    speechBuffer.addAll(frameBuffer.take(read))
                    speechDuration += frameDuration
                    silenceDuration = 0

                    // Phát partial score mỗi PARTIAL_INTERVAL_MS
                    if (speechDuration - lastPartialMs >= PARTIAL_INTERVAL_MS) {
                        lastPartialMs = speechDuration
                        emitPartialChunk(isFinal = false)
                    }
                }

                // ── Đang nói, bắt đầu im lặng ────────────
                state == RecordingState.SPEAKING && !isSpeech -> {
                    speechBuffer.addAll(frameBuffer.take(read))
                    silenceDuration += frameDuration

                    if (silenceDuration >= SILENCE_TIMEOUT_MS &&
                        speechDuration  >= MIN_SPEECH_MS) {
                        // Kết thúc câu — emit final chunk
                        emitPartialChunk(isFinal = true)
                        break
                    }
                }

                // ── Timeout tối đa ───────────────────────
                else -> {
                    if (totalDuration >= MAX_RECORD_MS && speechBuffer.isNotEmpty()) {
                        emitPartialChunk(isFinal = true)
                        break
                    }
                }
            }
        }

        withContext(Dispatchers.Main) { state = RecordingState.IDLE }
    }

    // ── Emit helpers ──────────────────────────

    private suspend fun emitPartialChunk(isFinal: Boolean) {
        val pcm = speechBuffer.toShortArray()
        val floats = pcmShortToFloat(pcm)
        val durationMs = pcm.size * 1000 / SAMPLE_RATE
        val chunk = AudioChunk(floats, durationMs, isFinal)

        withContext(Dispatchers.Main) {
            if (isFinal) {
                state = RecordingState.PROCESSING
                onSpeechEnd?.invoke(chunk)
            } else {
                onSpeechChunk?.invoke(chunk)
            }
        }
    }

    // ─────────────────────────────────────────
    // DSP helpers
    // ─────────────────────────────────────────

    /**
     * Root Mean Square — đo năng lượng frame.
     * PCM 16-bit range: -32768..32767
     * RMS > 500 ≈ giọng nói bình thường trong phòng yên tĩnh.
     */
    private fun computeRMS(buffer: ShortArray, length: Int): Float {
        var sum = 0.0
        for (i in 0 until length) {
            sum += buffer[i].toDouble() * buffer[i].toDouble()
        }
        return sqrt(sum / length).toFloat()
    }

    /**
     * Chuyển PCM 16-bit [-32768, 32767] → Float32 [-1.0, 1.0].
     * wav2vec2 yêu cầu input normalize về [-1, 1].
     */
    fun pcmShortToFloat(pcm: ShortArray): FloatArray {
        return FloatArray(pcm.size) { i -> pcm[i] / 32768.0f }
    }

    /**
     * Pre-emphasis filter — tăng cường tần số cao, giảm nhiễu thấp.
     * Công thức: y[t] = x[t] - α * x[t-1], α thường = 0.97
     * Áp dụng trước khi đưa vào wav2vec2 giúp tăng độ chính xác nhận dạng.
     */
    fun preEmphasis(signal: FloatArray, alpha: Float = 0.97f): FloatArray {
        val out = FloatArray(signal.size)
        out[0] = signal[0]
        for (i in 1 until signal.size) {
            out[i] = signal[i] - alpha * signal[i - 1]
        }
        return out
    }

    /**
     * Trim silence — cắt bỏ silence ở đầu và cuối.
     * Dùng sau khi thu âm xong để giảm kích thước input cho model.
     */
    fun trimSilence(signal: FloatArray, threshold: Float = 0.01f): FloatArray {
        var start = 0
        var end = signal.size - 1
        val frameLen = FRAME_SAMPLES

        // Tìm điểm đầu có tiếng
        while (start < end - frameLen) {
            val rms = computeRMSFloat(signal, start, frameLen)
            if (rms > threshold) break
            start += frameLen
        }

        // Tìm điểm cuối có tiếng
        while (end > start + frameLen) {
            val rms = computeRMSFloat(signal, end - frameLen, frameLen)
            if (rms > threshold) break
            end -= frameLen
        }

        return signal.copyOfRange(start, (end + 1).coerceAtMost(signal.size))
    }

    private fun computeRMSFloat(signal: FloatArray, offset: Int, length: Int): Float {
        var sum = 0.0
        for (i in offset until (offset + length).coerceAtMost(signal.size)) {
            sum += signal[i].toDouble() * signal[i].toDouble()
        }
        return sqrt(sum / length).toFloat()
    }
}
