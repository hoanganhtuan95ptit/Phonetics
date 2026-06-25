package com.simple.feature.pronunciation_assessment.domain.entities

/**
 * Đoạn audio đã được [com.simple.feature.pronunciation_assessment.domain.repositories.AudioRecorder]
 * cắt ra: realtime mỗi 500ms (partial) hoặc khi người dùng dừng nói (final).
 *
 * @param pcmFloat       PCM float32 đã normalize [-1, 1], 16 kHz mono
 * @param durationMs     độ dài audio tính bằng millisecond
 * @param isFinal        true = người dùng đã dừng nói
 * @param pauseCount     số lần dừng giữa câu mà VAD phát hiện
 * @param audioFilePath  đường dẫn WAV (chỉ có khi [isFinal] = true)
 */
data class AudioChunk(
    val pcmFloat: FloatArray,
    val durationMs: Int,
    val isFinal: Boolean,
    val pauseCount: Int = 0,
    val audioFilePath: String? = null,
)
