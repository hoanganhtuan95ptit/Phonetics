package com.simple.phonetics.ui.home.services.detect

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
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postValueIfActive
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.domain.usecase.detect.CheckSupportDetectUseCase
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.state.toSuccess
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DetectHomeViewModel(
    private val checkSupportDetectUseCase: CheckSupportDetectUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val isReverse: LiveData<Boolean> = MediatorLiveData(false)

    @VisibleForTesting
    val isSupportDetectState: LiveData<ResultState<Boolean>> = combineSourcesWithDiff(isReverse, inputLanguage, outputLanguage) {

        val isReverse = isReverse.get()
        val inputLanguage = inputLanguage.get()
        val outputLanguage = outputLanguage.get()

        val languageCode = if (isReverse) {
            outputLanguage.id
        } else {
            inputLanguage.id
        }

        val param = CheckSupportDetectUseCase.Param(languageCode = languageCode)

        checkSupportDetectUseCase.execute(param).collect { state ->

            postValue(state)

            logAnalytics("feature_detect_${state.javaClass.simpleName.lowercase()}")

            state.doSuccess {

                logAnalytics("feature_detect_${languageCode}_${it}")
            }

            state.doFailed {

                logCrashlytics("feature_detect", it)
            }
        }
    }.apply {

        this.asObject<MutableLiveData<ResultState<Boolean>>>().value = ResultState.Success(false)
    }

    val detectInfo: LiveData<DetectInfo> = combineSourcesWithDiff(isSupportDetectState) {

        val isSupportDetectState = isSupportDetectState.value ?: return@combineSourcesWithDiff

        if (isSupportDetectState is ResultState.Start) {

            return@combineSourcesWithDiff
        }


        val isSupportDetect = isSupportDetectState.toSuccess()?.data == true

        val info = DetectInfo(
            isShow = isSupportDetect
        )

        postValueIfActive(info)
    }

    init {

        detectInfo.asFlow().map { it.isShow }.distinctUntilChanged().launchCollect(viewModelScope) {

            logAnalytics("feature_detect_home_show_$it")
        }
    }

    fun updateReverse(it: Boolean) {

        isReverse.postDifferentValue(it)
    }

    data class DetectInfo(
        val isShow: Boolean
    )
}