/**
 * AudioRecorderImpl.kt
 *
 * Thu âm từ microphone Android, phát hiện giọng nói (VAD), và cắt audio
 * thành chunks để đưa vào wav2vec2.
 *
 * Luồng dữ liệu:
 *   AudioRecord (PCM 16-bit, 16 kHz)
 *     → VAD (energy-based)
 *       → onSpeechChunk(chunk)  — realtime mỗi 500 ms (partial)
 *       → onSpeechEnd(chunk)    — khi người dùng dừng nói (final)
 */

package com.simple.feature.pronunciation_assessment.data.audio

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import com.simple.feature.pronunciation_assessment.domain.entities.AudioChunk
import com.simple.feature.pronunciation_assessment.domain.entities.RecordingState
import com.simple.feature.pronunciation_assessment.domain.repositories.AudioRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sqrt

// ─────────────────────────────────────────────
// Constants
// ─────────────────────────────────────────────

private const val SAMPLE_RATE = 16_000             // wav2vec2 yêu cầu 16 kHz
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

// VAD
private const val FRAME_SIZE_MS = 20
private const val FRAME_SAMPLES = SAMPLE_RATE * FRAME_SIZE_MS / 1000 // 320 samples
private const val SILENCE_TIMEOUT_MS = 800
private const val MID_PAUSE_MS = 250
private const val MIN_SPEECH_MS = 200
private const val MAX_RECORD_MS = 15_000

private const val PARTIAL_INTERVAL_MS = 500
private const val INITIAL_BUFFER_CAPACITY = SAMPLE_RATE // ~1s

private const val WAV_FILE_NAME = "user_recording.wav"

// ─────────────────────────────────────────────
// AudioRecorderImpl
// ─────────────────────────────────────────────

class AudioRecorderImpl(private val context: Context) : AudioRecorder {

    // ── Callbacks ─────────────────────────────
    override var onSpeechChunk: ((AudioChunk) -> Unit)? = null
    override var onSpeechEnd: ((AudioChunk) -> Unit)? = null
    override var onStateChange: ((RecordingState) -> Unit)? = null
    override var onError: ((String) -> Unit)? = null

    // ── Internal state ────────────────────────
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // recordLoop chạy trên IO; callbacks hay đụng UI → marshal về Main.
    private val mainHandler = Handler(Looper.getMainLooper())
    private fun <T> ((T) -> Unit)?.invokeOnMain(value: T) {
        val cb = this ?: return
        if (Looper.myLooper() == Looper.getMainLooper()) cb(value)
        else mainHandler.post { cb(value) }
    }

    private val speechBuffer = FloatGrowBuffer(INITIAL_BUFFER_CAPACITY)
    private var state = RecordingState.IDLE
        set(value) {
            field = value
            onStateChange.invokeOnMain(value)
        }

    // ── Start / Stop ──────────────────────────

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun start() {
        if (state != RecordingState.IDLE) return

        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufSize = maxOf(minBuf * 2, FRAME_SAMPLES * 2 * 4)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufSize,
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

    override fun stop() {
        recordingJob?.cancel()
        recordingJob = null
        audioRecord?.apply {
            try { stop() } catch (_: IllegalStateException) {}
            release()
        }
        audioRecord = null
        state = RecordingState.IDLE
        speechBuffer.clear()
    }

    // ── Core recording loop ───────────────────

    private suspend fun recordLoop() = runCatching {
        val frameBuffer = ShortArray(FRAME_SAMPLES)
        val vad = EnergyVad()

        var silenceDuration = 0
        var speechDuration = 0
        var totalDuration = 0
        var lastPartialMs = 0
        var pauseCount = 0  // số lần im lặng giữa câu (≥ MID_PAUSE_MS, < SILENCE_TIMEOUT_MS)
        var inPause = false

        while (currentCoroutineContext().isActive) {
            val read = audioRecord?.read(frameBuffer, 0, FRAME_SAMPLES) ?: break
            if (read <= 0) continue

            val rms = computeRMS(frameBuffer, read)
            val isSpeech = vad.isSpeech(
                rms = rms,
                currentlySpeaking = state == RecordingState.SPEAKING,
            )
            val frameDuration = read * 1000 / SAMPLE_RATE

            totalDuration += frameDuration

            // ── Timeout tối đa: check trước, đảm bảo luôn trigger ─
            if (totalDuration >= MAX_RECORD_MS) {
                if (state == RecordingState.SPEAKING) {
                    speechBuffer.appendShortAsFloat(frameBuffer, read)
                }
                if (!speechBuffer.isEmpty()) {
                    // Có giọng nói → emit chunk cuối như bình thường.
                    emitChunk(isFinal = true, pauseCount = pauseCount)
                } else {
                    // Không phát hiện giọng nói trong suốt MAX_RECORD_MS.
                    withContext(Dispatchers.Main) { state = RecordingState.TIMEOUT }
                }
                break
            }

            when {
                // ── Đang nghe, phát hiện tiếng nói ──────
                state == RecordingState.LISTENING && isSpeech -> {
                    state = RecordingState.SPEAKING
                    speechBuffer.appendShortAsFloat(frameBuffer, read)
                    speechDuration = frameDuration
                    silenceDuration = 0
                    inPause = false
                }

                // ── Đang nói, vẫn có tiếng ──────────────
                state == RecordingState.SPEAKING && isSpeech -> {
                    speechBuffer.appendShortAsFloat(frameBuffer, read)
                    speechDuration += frameDuration

                    if (inPause && silenceDuration >= MID_PAUSE_MS) {
                        pauseCount++
                    }
                    silenceDuration = 0
                    inPause = false

                    if (speechDuration - lastPartialMs >= PARTIAL_INTERVAL_MS) {
                        lastPartialMs = speechDuration
                        emitChunk(isFinal = false, pauseCount = pauseCount)
                    }
                }

                // ── Đang nói, bắt đầu im lặng ────────────
                state == RecordingState.SPEAKING && !isSpeech -> {
                    speechBuffer.appendShortAsFloat(frameBuffer, read)
                    silenceDuration += frameDuration
                    if (silenceDuration >= MID_PAUSE_MS) inPause = true

                    if (silenceDuration >= SILENCE_TIMEOUT_MS &&
                        speechDuration >= MIN_SPEECH_MS
                    ) {
                        emitChunk(isFinal = true, pauseCount = pauseCount)
                        break
                    }
                }
            }
        }

        withContext(Dispatchers.Main) { state = RecordingState.IDLE }
    }

    // ── Emit helpers ──────────────────────────

    private suspend fun emitChunk(isFinal: Boolean, pauseCount: Int) {
        val snapshot = speechBuffer.snapshot()
        val durationMs = snapshot.size * 1000 / SAMPLE_RATE
        val filePath = if (isFinal) saveWav(snapshot) else null
        val chunk = AudioChunk(snapshot, durationMs, isFinal, pauseCount, filePath)

        withContext(Dispatchers.Main) {
            if (isFinal) {
                state = RecordingState.PROCESSING
                onSpeechEnd?.invoke(chunk)
            } else {
                onSpeechChunk?.invoke(chunk)
            }
        }
    }

    private fun saveWav(samples: FloatArray): String {
        val file = File(context.cacheDir, WAV_FILE_NAME)
        return WavWriter.write(file, samples, SAMPLE_RATE)
    }

    // ── DSP helpers (private) ─────────────────

    /** RMS — đo năng lượng frame PCM 16-bit. Dùng Long thay Double để nhanh hơn. */
    private fun computeRMS(buffer: ShortArray, length: Int): Float {
        if (length <= 0) return 0f
        var sum = 0L
        for (i in 0 until length) {
            val s = buffer[i].toInt()
            sum += (s * s).toLong()
        }
        return sqrt(sum.toDouble() / length).toFloat()
    }

    /**
     * VAD năng lượng với:
     *  - Cached threshold (chỉ tính lại khi noiseRms thay đổi)
     *  - Cập nhật noise nền asymmetric (tăng nhanh / giảm chậm)
     *  - Cap noiseRms để tránh drift làm ngưỡng tăng vô hạn
     *  - Hard reset counter giữ stop logic chắc chắn (silence đủ lâu là dừng)
     */
    private class EnergyVad(
        private val frameMs: Int = FRAME_SIZE_MS,
    ) {
        private var noiseRms = 300f

        // Cached thresholds — chỉ recompute khi noiseRms thay đổi.
        private var startThreshold = 0f
        private var stopThreshold = 0f

        private var speechFrames = 0
        private var silenceFrames = 0

        private val minSpeechFrames = 3      // 60ms
        private val minSilenceFrames = 5     // 100ms

        // Cap noise floor để tránh drift làm threshold cao bất thường.
        private val noiseRmsFloor = 100f
        private val noiseRmsCap = 1000f

        init {
            updateThresholds()
        }

        private fun updateThresholds() {
            startThreshold = maxOf(600f, noiseRms * 3.5f)
            stopThreshold = maxOf(350f, noiseRms * 2.0f)
        }

        fun isSpeech(rms: Float, currentlySpeaking: Boolean): Boolean {
            val rawSpeech = if (currentlySpeaking) rms > stopThreshold
                            else rms > startThreshold

            if (rawSpeech) {
                speechFrames++
                silenceFrames = 0
            } else {
                silenceFrames++
                speechFrames = 0

                // Cập nhật noise nền chỉ khi không đang nói.
                // Asymmetric: tăng nhanh (α=0.1) khi noise cao, giảm chậm (α=0.02).
                if (!currentlySpeaking) {
                    val alpha = if (rms > noiseRms) 0.1f else 0.02f
                    val newNoise = (noiseRms * (1f - alpha) + rms * alpha)
                        .coerceIn(noiseRmsFloor, noiseRmsCap)
                    if (newNoise != noiseRms) {
                        noiseRms = newNoise
                        updateThresholds()
                    }
                }
            }

            return if (currentlySpeaking) {
                silenceFrames < minSilenceFrames
            } else {
                speechFrames >= minSpeechFrames
            }
        }
    }
}
