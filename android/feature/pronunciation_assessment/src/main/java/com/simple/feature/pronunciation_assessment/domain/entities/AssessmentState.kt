package com.simple.feature.pronunciation_assessment.domain.entities

/** Trạng thái pipeline chấm phát âm (orchestrator). */
enum class AssessmentState {
    /** Chưa load model. */
    UNINITIALIZED,

    /** Sẵn sàng — đã load model + reference. */
    READY,

    /** Đang chờ giọng nói. */
    LISTENING,

    /** Đang ghi âm. */
    RECORDING,

    /** Đang chạy wav2vec2 + scorer. */
    PROCESSING,

    /** Có lỗi. */
    ERROR,
}
