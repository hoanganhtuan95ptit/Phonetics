/**
 * AudioProcessor.kt
 *
 * Thu âm từ microphone Android, phát hiện giọng nói (VAD),
 * và cắt audio thành chunks để đưa vào wav2vec2.
 *
 * Luồng dữ liệu:
 *   AudioRecord (PCM 16-bit, 16kHz)
 *     → VAD (energy-based)
 *       → onSpeechChunk(chunk)  — realtime mỗi 500ms (partial)
 *       → onSpeechEnd(chunk)    — khi người dùng dừng nói (final)
 *
 * Tối ưu so với bản cũ:
 *   - speechBuffer dùng PRIMITIVE FloatArray (auto-grow), không
 *     phải MutableList<Short> (16kHz × 15s = 240k boxed Short objects
 *     → GC stall + ~6MB heap). Bản mới ~960KB float thuần.
 *   - Convert short → float NGAY khi append, tránh duyệt lại 2 lần.
 *   - Snapshot copyOfRange(0, size) thay vì toShortArray() rồi
 *     pcmShortToFloat() — tiết kiệm 1 lần allocate.
 *
 * Cách dùng:
 *   val processor = AudioProcessor(context)
 *   processor.onSpeechChunk = { pcm -> scorer.scorePartial(pcm) }
 *   processor.onSpeechEnd   = { pcm -> scorer.scoreFinal(pcm) }
 *   processor.start()
 *   // ... người dùng nói ...
 *   processor.stop()
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
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
private const val ENERGY_THRESHOLD  = 500.0f    // RMS threshold
private const val SILENCE_TIMEOUT_MS = 800      // ms im lặng → coi là kết thúc câu
private const val MID_PAUSE_MS      = 250       // ms im lặng giữa câu → tính là 1 lần dừng
private const val MIN_SPEECH_MS     = 200       // ms tối thiểu để tính là có giọng nói
private const val MAX_RECORD_MS     = 15_000    // ms tối đa 1 lần ghi

// Partial scoring interval
private const val PARTIAL_INTERVAL_MS = 500     // ms — cứ 500ms emit partial chunk

// Capacity ban đầu cho speechBuffer = ~1 giây audio
private const val INITIAL_BUFFER_CAPACITY = SAMPLE_RATE

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
    val isFinal: Boolean,        // true = người dùng đã dừng nói
    val pauseCount: Int = 0,     // số lần dừng giữa câu (VAD phát hiện)
    val audioFilePath: String? = null // đường dẫn WAV (chỉ có khi isFinal = true)
)

// ─────────────────────────────────────────────
// FloatBuffer mở rộng tay (tránh boxing)
// ─────────────────────────────────────────────

/**
 * Buffer float thuần — auto-grow như ArrayList nhưng KHÔNG box.
 * Dùng cho speechBuffer (16kHz × 15s = 240k samples).
 */
private class FloatGrowBuffer(initialCapacity: Int = INITIAL_BUFFER_CAPACITY) {
    private var data = FloatArray(initialCapacity)
    var size: Int = 0
        private set

    fun appendShortAsFloat(src: ShortArray, length: Int) {
        ensureCapacity(size + length)
        var i = size
        for (k in 0 until length) {
            data[i++] = src[k] / 32768.0f
        }
        size = i
    }

    fun snapshot(): FloatArray = data.copyOf(size)

    fun clear() {
        size = 0
        // không shrink data — giữ capacity để lần sau khỏi allocate
    }

    fun isEmpty(): Boolean = size == 0

    private fun ensureCapacity(min: Int) {
        if (min <= data.size) return
        var newCap = data.size + (data.size shr 1)  // grow ×1.5
        if (newCap < min) newCap = min
        data = data.copyOf(newCap)
    }
}

// ─────────────────────────────────────────────
// AudioProcessor
// ─────────────────────────────────────────────

class AudioProcessor(private val context: Context) {

    // ── Callbacks ─────────────────────────────
    var onSpeechChunk: ((AudioChunk) -> Unit)? = null
    var onSpeechEnd:   ((AudioChunk) -> Unit)? = null
    var onStateChange: ((RecordingState) -> Unit)? = null
    var onError:       ((String) -> Unit)? = null

    // ── Internal state ────────────────────────
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Marshal callbacks về main thread — recordLoop chạy IO, callbacks
    // hay đụng UI (setActivated, setText...) sẽ crash nếu không marshal.
    private val mainHandler = Handler(Looper.getMainLooper())
    private fun <T> ((T) -> Unit)?.invokeOnMain(value: T) {
        val cb = this ?: return
        if (Looper.myLooper() == Looper.getMainLooper()) cb(value)
        else mainHandler.post { cb(value) }
    }

    private val speechBuffer = FloatGrowBuffer()
    private var state = RecordingState.IDLE
        set(value) { field = value; onStateChange.invokeOnMain(value) }

    // ── Start / Stop ──────────────────────────

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        if (state != RecordingState.IDLE) return

        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufSize = maxOf(minBuf * 2, FRAME_SAMPLES * 2 * 4)

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

    private suspend fun recordLoop() {
        val frameBuffer     = ShortArray(FRAME_SAMPLES)
        var silenceDuration = 0
        var speechDuration  = 0
        var totalDuration   = 0
        var lastPartialMs   = 0
        var pauseCount      = 0   // số lần im lặng giữa câu (>= MID_PAUSE_MS, < SILENCE_TIMEOUT_MS)
        var inPause         = false  // đang trong khoảng dừng giữa câu

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
                    speechBuffer.appendShortAsFloat(frameBuffer, read)
                    speechDuration = frameDuration
                    silenceDuration = 0
                    inPause = false
                }

                // ── Đang nói, vẫn có tiếng ──────────────
                state == RecordingState.SPEAKING && isSpeech -> {
                    speechBuffer.appendShortAsFloat(frameBuffer, read)
                    speechDuration += frameDuration

                    // Kết thúc một khoảng dừng giữa câu
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
                        speechDuration  >= MIN_SPEECH_MS) {
                        emitChunk(isFinal = true, pauseCount = pauseCount)
                        break
                    }
                }

                // ── Timeout tối đa ───────────────────────
                else -> {
                    if (totalDuration >= MAX_RECORD_MS && !speechBuffer.isEmpty()) {
                        emitChunk(isFinal = true, pauseCount = pauseCount)
                        break
                    }
                }
            }
        }

        withContext(Dispatchers.Main) { state = RecordingState.IDLE }
    }

    // ── Emit helpers ──────────────────────────

    private suspend fun emitChunk(isFinal: Boolean, pauseCount: Int = 0) {
        val snapshot   = speechBuffer.snapshot()
        val durationMs = snapshot.size * 1000 / SAMPLE_RATE
        val filePath   = if (isFinal) saveWav(snapshot) else null
        val chunk      = AudioChunk(snapshot, durationMs, isFinal, pauseCount, filePath)

        withContext(Dispatchers.Main) {
            if (isFinal) {
                state = RecordingState.PROCESSING
                onSpeechEnd?.invoke(chunk)
            } else {
                onSpeechChunk?.invoke(chunk)
            }
        }
    }

    /**
     * Lưu audio dạng WAV 16-bit mono 16kHz vào cache với tên cố định
     * "user_recording.wav" — ghi đè mỗi lần người dùng đọc xong.
     *
     * @return đường dẫn tuyệt đối của file đã lưu
     */
    private fun saveWav(samples: FloatArray): String {
        val file      = File(context.cacheDir, "user_recording.wav")
        val dataSize  = samples.size * 2        // 16-bit = 2 bytes/sample
        val buf       = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

        // ── RIFF header ─────────────────────────
        buf.put("RIFF".toByteArray())
        buf.putInt(36 + dataSize)               // ChunkSize
        buf.put("WAVE".toByteArray())
        // ── fmt subchunk ────────────────────────
        buf.put("fmt ".toByteArray())
        buf.putInt(16)                          // Subchunk1Size (PCM)
        buf.putShort(1)                         // AudioFormat = PCM
        buf.putShort(1)                         // NumChannels  = mono
        buf.putInt(SAMPLE_RATE)                 // SampleRate
        buf.putInt(SAMPLE_RATE * 2)             // ByteRate
        buf.putShort(2)                         // BlockAlign
        buf.putShort(16)                        // BitsPerSample
        // ── data subchunk ───────────────────────
        buf.put("data".toByteArray())
        buf.putInt(dataSize)
        for (f in samples) {
            buf.putShort((f * 32767f).toInt().coerceIn(-32768, 32767).toShort())
        }

        FileOutputStream(file).use { it.write(buf.array()) }
        return file.absolutePath
    }

    // ─────────────────────────────────────────
    // DSP helpers (vẫn public — Wav2Vec2SpeechRecognizer còn dùng)
    // ─────────────────────────────────────────

    /**
     * Root Mean Square — đo năng lượng frame PCM 16-bit.
     * RMS > 500 ≈ giọng nói bình thường trong phòng yên tĩnh.
     */
    private fun computeRMS(buffer: ShortArray, length: Int): Float {
        var sum = 0.0
        for (i in 0 until length) {
            val s = buffer[i].toDouble()
            sum += s * s
        }
        return sqrt(sum / length).toFloat()
    }

    /**
     * Chuyển PCM 16-bit [-32768, 32767] → Float32 [-1.0, 1.0].
     */
    fun pcmShortToFloat(pcm: ShortArray): FloatArray {
        return FloatArray(pcm.size) { i -> pcm[i] / 32768.0f }
    }

    /**
     * Pre-emphasis filter — y[t] = x[t] - α * x[t-1].
     *
     * CHÚ Ý: KHÔNG dùng cho Wav2Vec2! Model end-to-end này được train
     * trên raw waveform, pre-emphasis sẽ làm méo phân bố input. Giữ
     * lại chỉ vì Wav2Vec2SpeechRecognizer (legacy) còn gọi.
     */
    fun preEmphasis(signal: FloatArray, alpha: Float = 0.97f): FloatArray {
        val out = FloatArray(signal.size)
        if (signal.isEmpty()) return out
        out[0] = signal[0]
        for (i in 1 until signal.size) {
            out[i] = signal[i] - alpha * signal[i - 1]
        }
        return out
    }

    /**
     * Trim silence — cắt bỏ silence ở đầu và cuối.
     * Như preEmphasis, không nên dùng cho partial chunk của Wav2Vec2
     * (làm CTC alignment dao động). Vẫn public cho Wav2Vec2SpeechRecognizer.
     */
    fun trimSilence(signal: FloatArray, threshold: Float = 0.01f): FloatArray {
        var start = 0
        var end = signal.size - 1
        val frameLen = FRAME_SAMPLES

        while (start < end - frameLen) {
            val rms = computeRMSFloat(signal, start, frameLen)
            if (rms > threshold) break
            start += frameLen
        }

        while (end > start + frameLen) {
            val rms = computeRMSFloat(signal, end - frameLen, frameLen)
            if (rms > threshold) break
            end -= frameLen
        }

        return signal.copyOfRange(start, (end + 1).coerceAtMost(signal.size))
    }

    private fun computeRMSFloat(signal: FloatArray, offset: Int, length: Int): Float {
        var sum = 0.0
        val limit = (offset + length).coerceAtMost(signal.size)
        for (i in offset until limit) {
            sum += signal[i].toDouble() * signal[i].toDouble()
        }
        return sqrt(sum / length).toFloat()
    }
}
