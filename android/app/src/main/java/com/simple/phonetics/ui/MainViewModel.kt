package com.simple.phonetics.ui

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModels.BaseViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.simple.analytics.logAnalytics
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.phonetics.BuildConfig
import com.simple.phonetics.domain.usecase.GetTranslateAsyncUseCase
import com.simple.phonetics.domain.usecase.SyncDataUseCase
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val syncDataUseCase: SyncDataUseCase,
    private val getLanguageInputUseCase: GetLanguageInputUseCase,
    private val getWordStateAsyncUseCase: GetWordStateAsyncUseCase,
    private val getTranslateAsyncUseCase: GetTranslateAsyncUseCase,
    private val getLanguageInputAsyncUseCase: GetLanguageInputAsyncUseCase,
    private val getLanguageOutputAsyncUseCase: GetLanguageOutputAsyncUseCase,
    private val getLanguageSupportAsyncUseCase: GetLanguageSupportAsyncUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val sync: LiveData<ResultState<Unit>> = mediatorLiveData {

        syncDataUseCase.execute().collect {

        }
    }

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
            logAnalytics("input_language_code_${it.id}")
        }
    }

    val outputLanguage: LiveData<Language> = mediatorLiveData {

        getLanguageOutputAsyncUseCase.execute().collect {

            appOutputLanguage.tryEmit(it)
            logAnalytics("output_language_code_${it.id}")
        }
    }

    val languageInputLanguage: LiveData<Language?> = mediatorLiveData {

        postValue(getLanguageInputUseCase.execute())
    }

    init {

        sync.asFlow().launchIn(viewModelScope)

        wordAsync.asFlow().launchIn(viewModelScope)
        languageAsync.asFlow().launchIn(viewModelScope)
        translateAsync.asFlow().launchIn(viewModelScope)

        inputLanguage.asFlow().launchIn(viewModelScope)
        outputLanguage.asFlow().launchIn(viewModelScope)

        viewModelScope.launch(handler + Dispatchers.IO) {

            logAnalytics("version_name_${BuildConfig.VERSION_NAME.replace(".", "_")}")
        }
    }
}