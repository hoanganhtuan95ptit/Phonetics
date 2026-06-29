package com.simple.feature.pronunciation_assessment.ui

import androidx.lifecycle.ViewModel
import com.simple.feature.pronunciation_assessment.domain.repositories.PronunciationAssessmentRepository

class GlobalPronunciationViewModel: ViewModel() {

    override fun onCleared() {
        super.onCleared()
        PronunciationAssessmentRepository.instance.release()
    }
}