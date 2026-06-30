package com.simple.feature.pronunciation_assessment.domain.usecase

import android.Manifest
import androidx.annotation.RequiresPermission
import com.simple.feature.pronunciation_assessment.domain.entities.AssessmentEvent
import com.simple.feature.pronunciation_assessment.domain.repositories.PronunciationAssessmentRepository
import kotlinx.coroutines.flow.Flow

/**
 * Bắt đầu nghe + chấm phát âm. Trả về [Flow] phát [AssessmentEvent]
 * — collector nhận partial, recordEnded, final, stateChanged, error.
 */
class StartAssessmentUseCase(private val repository: PronunciationAssessmentRepository, ) {

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun execute(referenceWords: List<Pair<String, List<String>>>): Flow<AssessmentEvent> {

        return repository.start(referenceWords)
    }

    companion object{
        val instance by lazy {
            StartAssessmentUseCase(PronunciationAssessmentRepository.instance)
        }
    }
}
