package com.simple.phonetics.ui

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModels.BaseViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.phonetics.domain.usecase.GetTranslateAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageOutputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageSupportAsyncUseCase
import com.simple.phonetics.domain.usecase.word.GetWordStateAsyncUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.utils.appInputLanguage
import com.simple.phonetics.utils.appOutputLanguage
import com.simple.phonetics.utils.appTranslate
import com.simple.state.ResultState
import kotlinx.coroutines.flow.launchIn

class MainViewModel(
    private val getLanguageInputUseCase: GetLanguageInputUseCase,
    private val getWordStateAsyncUseCase: GetWordStateAsyncUseCase,
    private val getTranslateAsyncUseCase: GetTranslateAsyncUseCase,
    private val getLanguageInputAsyncUseCase: GetLanguageInputAsyncUseCase,
    private val getLanguageOutputAsyncUseCase: GetLanguageOutputAsyncUseCase,
    private val getLanguageSupportAsyncUseCase: GetLanguageSupportAsyncUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val wordAsync: LiveData<ResultState<Int>> = mediatorLiveData {

        getWordStateAsyncUseCase.execute().collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val languageAsync: LiveData<ResultState<List<Language>>> = mediatorLiveData {

        getLanguageSupportAsyncUseCase.execute(GetLanguageSupportAsyncUseCase.Param(sync = true)).collect {

            postValue(it)
        }
    }

    @VisibleForTesting
    val translateAsync: LiveData<Map<String, String>> = mediatorLiveData {

        getTranslateAsyncUseCase.execute().collect {

            appTranslate.tryEmit(it)
        }
    }

    val inputLanguage: LiveData<Language> = mediatorLiveData {

        getLanguageInputAsyncUseCase.execute().collect {

            appInputLanguage.tryEmit(it)
        }
    }

    val outputLanguage: LiveData<Language> = mediatorLiveData {

        getLanguageOutputAsyncUseCase.execute().collect {

            appOutputLanguage.tryEmit(it)
        }
    }

    val languageInputLanguage: LiveData<Language?> = mediatorLiveData {

        postValue(getLanguageInputUseCase.execute())
    }

    init {

        wordAsync.asFlow().launchIn(viewModelScope)
        languageAsync.asFlow().launchIn(viewModelScope)
        translateAsync.asFlow().launchIn(viewModelScope)

        inputLanguage.asFlow().launchIn(viewModelScope)
        outputLanguage.asFlow().launchIn(viewModelScope)
    }
}