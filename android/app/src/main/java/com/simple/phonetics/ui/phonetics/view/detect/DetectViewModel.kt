package com.simple.phonetics.ui.phonetics.view.detect

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postDifferentValueIfActive
import com.simple.phonetics.domain.usecase.DetectStateUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageOutputAsyncUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.ui.base.CommonViewModel

class DetectViewModel(
    private val detectStateUseCase: DetectStateUseCase,
    private val getLanguageInputAsyncUseCase: GetLanguageInputAsyncUseCase,
    private val getLanguageOutputAsyncUseCase: GetLanguageOutputAsyncUseCase
) : CommonViewModel() {

    @VisibleForTesting
    val isReverse: LiveData<Boolean> = MediatorLiveData(false)

    @VisibleForTesting
    val inputLanguage: LiveData<Language> = mediatorLiveData {

        getLanguageInputAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val outputLanguage: LiveData<Language> = mediatorLiveData {

        getLanguageOutputAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val isSupportDetect: LiveData<Boolean> = combineSources(isReverse, inputLanguage, outputLanguage) {

        val isReverse = isReverse.get()
        val inputLanguage = inputLanguage.get()
        val outputLanguage = outputLanguage.get()

        val languageCode = if (isReverse) {
            outputLanguage.id
        } else {
            inputLanguage.id
        }

        postDifferentValueIfActive(detectStateUseCase.execute(DetectStateUseCase.Param(languageCode = languageCode)))
    }

    val detectInfo: LiveData<DetectInfo> = combineSources(isSupportDetect) {

        val isSupportDetect = isSupportDetect.get()

        val info = DetectInfo(
            isShow = isSupportDetect
        )

        postDifferentValueIfActive(info)
    }

    fun updateReverse(it: Boolean) {

        isReverse.postDifferentValue(it)
    }

    data class DetectInfo(
        val isShow: Boolean
    )
}