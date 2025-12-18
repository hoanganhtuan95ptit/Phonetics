package com.simple.phonetics.ui.home.services.microphone

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.simple.analytics.logAnalytics
import com.simple.core.utils.extentions.asObject
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.phonetics.domain.usecase.speak.CheckSupportSpeakUseCase
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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
    }.apply {

        asObject<MutableLiveData<Boolean>>().value = false
    }

    val microphoneInfo: LiveData<MicrophoneInfo> = combineSourcesWithDiff(isSupportSpeak) {

        val isSupportSpeak = isSupportSpeak.get()

        val info = MicrophoneInfo(
            isShow = isSupportSpeak
        )

        postValueIfActive(info)
    }

    init {

        microphoneInfo.asFlow().map { it.isShow }.distinctUntilChanged().launchCollect(viewModelScope) {

            logAnalytics("feature_microphone_home_show_$it")
        }
    }

    fun updateReverse(it: Boolean) {

        isReverse.postValue(it)
    }

    data class MicrophoneInfo(
        val isShow: Boolean
    )
}