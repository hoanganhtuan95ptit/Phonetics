package com.simple.feature.pronunciation_assessment.domain.entities

/** Trạng thái của [com.simple.feature.pronunciation_assessment.domain.repositories.AudioRecorder]. */
enum class RecordingState {
    /** Chưa bắt đầu. */
    IDLE,

    /** Đang thu âm, chờ giọng nói. */
    LISTENING,

    /** Phát hiện giọng nói, đang thu. */
    SPEAKING,

    /** Đã dừng thu, đang xử lý chunk cuối. */
    PROCESSING,
}
