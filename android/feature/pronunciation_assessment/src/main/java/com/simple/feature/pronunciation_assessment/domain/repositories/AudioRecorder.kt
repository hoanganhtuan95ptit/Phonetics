package com.simple.feature.pronunciation_assessment.domain.repositories

import android.Manifest
import androidx.annotation.RequiresPermission
import com.simple.feature.pronunciation_assessment.domain.entities.AudioChunk
import com.simple.feature.pronunciation_assessment.domain.entities.RecordingState

/**
 * Thu âm từ microphone, phát hiện giọng nói bằng VAD và cắt audio thành
 * [AudioChunk]. Có callback realtime cho partial và final.
 *
 * Lifecycle: gọi [start] → callbacks bắn ra → [stop] để giải phóng mic.
 *
 * Lưu ý: callbacks được marshal về Main thread bởi implementation.
 */
interface AudioRecorder {

    /** Bắn mỗi ~500ms khi đang nói (partial). */
    var onSpeechChunk: ((AudioChunk) -> Unit)?

    /** Bắn 1 lần khi người dùng dừng nói (final). */
    var onSpeechEnd: ((AudioChunk) -> Unit)?

    /** Bắn khi trạng thái thu âm thay đổi. */
    var onStateChange: ((RecordingState) -> Unit)?

    /** Bắn khi có lỗi (init mic thất bại, …). */
    var onError: ((String) -> Unit)?

    /** Bắt đầu thu âm. Idempotent — gọi lại khi đang chạy là no-op. */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start()

    /** Dừng thu, giải phóng mic. */
    fun stop()
}
