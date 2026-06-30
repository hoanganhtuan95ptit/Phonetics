package com.simple.feature.pronunciation_assessment.domain.usecase

import com.simple.feature.pronunciation_assessment.domain.repositories.PronunciationAssessmentRepository
import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow

/**
 * Load model cho pipeline chấm phát âm.
 *
 * Repository tự chạy phần load model trên IO và trả progress qua Flow.
 */
class PrepareAssessmentUseCase(private val repository: PronunciationAssessmentRepository) {

    fun execute(param: Param): Flow<ResultState<Int>> = repository.prepare(useGPU = param.useGPU)

    data class Param(
        val useGPU: Boolean = false,
    )

    companion object {

        val instance by lazy {
            PrepareAssessmentUseCase(PronunciationAssessmentRepository.instance)
        }
    }
}
