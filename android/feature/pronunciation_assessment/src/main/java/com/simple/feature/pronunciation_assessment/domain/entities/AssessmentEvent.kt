package com.simple.feature.pronunciation_assessment.domain.entities

import com.simple.phonetics.entities.SentenceScore

/**
 * Event do [com.simple.feature.pronunciation_assessment.domain.usecase.StartAssessmentUseCase]
 * phát ra trong quá trình chấm phát âm.
 */
sealed class AssessmentEvent {

    /** Trạng thái pipeline thay đổi. */
    data class StateChanged(val state: AssessmentState) : AssessmentEvent()

    /** Kết quả chấm partial (~500ms một lần khi đang nói). */
    data class Partial(val score: SentenceScore) : AssessmentEvent()

    /** Người dùng vừa dừng nói — chuẩn bị inference final. */
    data object RecordEnded : AssessmentEvent()

    /** Kết quả chấm final sau khi inference xong. */
    data class Final(val score: SentenceScore) : AssessmentEvent()

    /** Có lỗi xảy ra. */
    data class Error(val message: String) : AssessmentEvent()
}
