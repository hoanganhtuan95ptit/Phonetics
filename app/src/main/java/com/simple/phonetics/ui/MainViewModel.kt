package com.simple.phonetics.ui

import androidx.lifecycle.LiveData
import com.simple.coreapp.ui.base.viewmodels.BaseViewModel
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.liveData
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.translate.data.usecase.TranslateUseCase

class MainViewModel(
    private val translateUseCase: TranslateUseCase,

    private val languageRepository: LanguageRepository
) : BaseViewModel() {

    val inputLanguageCode: LiveData<String> = liveData {

        languageRepository.getLanguageInputAsync().collect {

            postValue(it.code)
        }
    }

    val outputLanguageCode: LiveData<String> = liveData {

        languageRepository.getLanguageOutputAsync().collect {

            postValue(it.code)
        }
    }

    val translateState: LiveData<ResultState<Boolean>> = combineSources(inputLanguageCode, outputLanguageCode) {

        postValue(ResultState.Start)

        val inputLanguageCode = inputLanguageCode.get()

        val outputLanguageCode = outputLanguageCode.get()

        translateUseCase.execute(TranslateUseCase.Param(listOf("hello"), inputLanguageCode, outputLanguageCode)).let { state ->

            state.doSuccess {

                postValue(ResultState.Success(true))
            }

            state.doFailed {

                postValue(ResultState.Failed(it))
            }
        }
    }
}