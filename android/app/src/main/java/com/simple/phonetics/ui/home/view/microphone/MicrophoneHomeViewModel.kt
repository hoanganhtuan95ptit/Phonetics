package com.simple.phonetics.ui.home.view.microphone

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.phonetics.domain.usecase.speak.CheckSupportSpeakUseCase
import com.simple.phonetics.ui.base.fragments.BaseViewModel

class MicrophoneHomeViewModel(
    private val checkSupportSpeakUseCase: CheckSupportSpeakUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val isReverse: LiveData<Boolean> = MediatorLiveData(false)

    @VisibleForTesting
    override val isSupportSpeak: LiveData<Boolean> = combineSourcesWithDiff(isReverse, inputLanguage, outputLanguage) {

        val isReverse = isReverse.get()
        val inputLanguage = inputLanguage.get()
        val outputLanguage = outputLanguage.get()

        val languageCode = if (isReverse) {
            outputLanguage.id
        } else {
            inputLanguage.id
        }

        postValue(checkSupportSpeakUseCase.execute(CheckSupportSpeakUseCase.Param(languageCode = languageCode)))
    }

    val microphoneInfo: LiveData<MicrophoneInfo> = combineSourcesWithDiff(isSupportSpeak) {

        val isSupportSpeak = isSupportSpeak.get()

        val info = MicrophoneInfo(
            isShow = isSupportSpeak
        )

        postValueIfActive(info)
    }

    fun updateReverse(it: Boolean) {

        isReverse.postValue(it)
    }

    data class MicrophoneInfo(
        val isShow: Boolean
    )
}