package com.simple.feature.pronunciation_assessment.domain.usecase

import com.simple.feature.pronunciation_assessment.domain.repositories.PronunciationAssessmentRepository

/**
 * Load model + set câu reference cho pipeline chấm phát âm.
 *
 * Blocking — gọi trên IO dispatcher.
 */
class PrepareAssessmentUseCase(
    private val repository: PronunciationAssessmentRepository,
) {

    suspend fun execute(param: Param) {
        repository.prepare(
            reference = param.reference,
            useGPU = param.useGPU,
            onProgress = param.onProgress,
        )
    }

    data class Param(
        /** List cặp (word, IPA phonemes). */
        val reference: List<Pair<String, List<String>>>,
        val useGPU: Boolean = false,
        /** Tiến trình tải model 0–100. Gọi trên IO dispatcher. */
        val onProgress: ((percent: Int) -> Unit)? = null,
    )
}
