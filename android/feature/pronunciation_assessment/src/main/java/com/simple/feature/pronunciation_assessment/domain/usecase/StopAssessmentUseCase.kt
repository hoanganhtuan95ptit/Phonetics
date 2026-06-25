package com.simple.feature.pronunciation_assessment.domain.usecase

import com.simple.feature.pronunciation_assessment.domain.repositories.PronunciationAssessmentRepository

/** Dừng pipeline thủ công, giải phóng mic. */
class StopAssessmentUseCase(
    private val repository: PronunciationAssessmentRepository,
) {

    fun execute() {
        repository.stop()
    }
}
