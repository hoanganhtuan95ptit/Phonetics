package com.simple.phonetics.ui

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.simple.analytics.logAnalytics
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.phonetics.BuildConfig
import com.simple.phonetics.domain.usecase.SyncDataUseCase
import com.simple.phonetics.utils.exts.awaitResume
import com.simple.phonetics.utils.exts.combineSourcesWithDiff
import com.simple.phonetics.utils.exts.mutableSharedFlow
import com.simple.phonetics.utils.exts.value
import com.simple.state.ResultState
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val syncDataUseCase: SyncDataUseCase
) : com.simple.phonetics.ui.base.fragments.BaseViewModel() {

    @VisibleForTesting
    val sync: LiveData<ResultState<Unit>> = mediatorLiveData {

        ProcessLifecycleOwner.get().awaitResume()

        syncDataUseCase.execute().collect {

        }
    }


    val initCompleted = mutableSharedFlow<Boolean> {

    }

    val openLanguage: Flow<Boolean> = combineSourcesWithDiff( inputLanguageFlow) {

        emit(inputLanguageFlow.value == null)
    }


    init {

        sync.asFlow().launchIn(viewModelScope)

        inputLanguageFlow.launchCollect(viewModelScope) {

            logAnalytics("input_language_code_${it?.id}")
        }

        outputLanguageFlow.launchCollect(viewModelScope) {

            logAnalytics("output_language_code_${it.id}")
        }

        viewModelScope.launch(handler + Dispatchers.IO) {

            logAnalytics("version_name_${BuildConfig.VERSION_NAME.replace(".", "_")}")
        }
    }

    fun initCompleted() {

        initCompleted.tryEmit(true)
    }
}