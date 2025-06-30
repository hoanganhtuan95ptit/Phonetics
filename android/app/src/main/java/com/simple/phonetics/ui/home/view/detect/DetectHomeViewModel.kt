package com.simple.phonetics.ui.home.view.detect

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.phonetics.domain.usecase.detect.CheckSupportDetectUseCase
import com.simple.phonetics.ui.base.fragments.BaseViewModel

class DetectHomeViewModel(
    private val checkSupportDetectUseCase: CheckSupportDetectUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val isReverse: LiveData<Boolean> = MediatorLiveData(false)

    @VisibleForTesting
    val isSupportDetect: LiveData<Boolean> = combineSourcesWithDiff(isReverse, inputLanguage, outputLanguage) {

        val isReverse = isReverse.get()
        val inputLanguage = inputLanguage.get()
        val outputLanguage = outputLanguage.get()

        val languageCode = if (isReverse) {
            outputLanguage.id
        } else {
            inputLanguage.id
        }

        postValueIfActive(checkSupportDetectUseCase.execute(CheckSupportDetectUseCase.Param(languageCode = languageCode)))
    }

    val detectInfo: LiveData<DetectInfo> = combineSourcesWithDiff(isSupportDetect) {

        val isSupportDetect = isSupportDetect.get()

        val info = DetectInfo(
            isShow = isSupportDetect
        )

        postValueIfActive(info)
    }

    fun updateReverse(it: Boolean) {

        isReverse.postValue(it)
    }

    data class DetectInfo(
        val isShow: Boolean
    )
}