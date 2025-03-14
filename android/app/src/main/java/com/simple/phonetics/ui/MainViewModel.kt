package com.simple.phonetics.ui

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModels.BaseViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.phonetics.domain.usecase.GetTranslateAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputUseCase
import com.simple.phonetics.domain.usecase.word.GetWordStateAsyncUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.utils.appTranslate
import com.simple.state.ResultState
import kotlinx.coroutines.flow.launchIn

class MainViewModel(
    private val getLanguageInputUseCase: GetLanguageInputUseCase,
    private val getWordStateAsyncUseCase: GetWordStateAsyncUseCase,
    private val getTranslateAsyncUseCase: GetTranslateAsyncUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val wordSync: LiveData<ResultState<Int>> = mediatorLiveData {

        postValue(ResultState.Start)

        getWordStateAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val keyTranslateSync: LiveData<Map<String, String>> = mediatorLiveData {

        getTranslateAsyncUseCase.execute().collect {

            appTranslate.tryEmit(it)
        }
    }

    val languageInputLanguage: LiveData<Language?> = mediatorLiveData {

        postValue(getLanguageInputUseCase.execute())
    }

    init {

        wordSync.asFlow().launchIn(viewModelScope)
        keyTranslateSync.asFlow().launchIn(viewModelScope)
    }
}