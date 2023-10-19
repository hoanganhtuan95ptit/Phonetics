package com.simple.phonetics.ui

import androidx.lifecycle.LiveData
import com.simple.coreapp.ui.base.viewmodels.BaseViewModel
import com.simple.coreapp.utils.extentions.combineSources
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.liveData
import com.simple.phonetics.domain.entities.Language
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageOutputAsyncUseCase
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.translate.data.usecase.TranslateUseCase

class MainViewModel(
    private val translateUseCase: TranslateUseCase,

    private val getLanguageInputAsyncUseCase: GetLanguageInputAsyncUseCase,
    private val getLanguageOutputAsyncUseCase: GetLanguageOutputAsyncUseCase
) : BaseViewModel() {

    val inputLanguage: LiveData<Language> = liveData {

        getLanguageInputAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    val outputLanguage: LiveData<Language> = liveData {

        getLanguageOutputAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    val translateState: LiveData<ResultState<Boolean>> = combineSources(inputLanguage, outputLanguage) {

        postValue(ResultState.Start)

        val inputLanguageCode = inputLanguage.get().code

        val outputLanguageCode = outputLanguage.get().code

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