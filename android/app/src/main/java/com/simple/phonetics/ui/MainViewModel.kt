package com.simple.phonetics.ui

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.simple.analytics.logAnalytics
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.phonetics.domain.usecase.SyncDataUseCase
import com.simple.phonetics.utils.exts.awaitResume
import com.simple.phonetics.utils.exts.mutableSharedFlow
import com.simple.state.ResultState
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.flow.launchIn

class MainViewModel(
    private val syncDataUseCase: SyncDataUseCase
) : com.simple.phonetics.ui.base.fragments.BaseViewModel() {

    @VisibleForTesting
    val sync: LiveData<ResultState<Unit>> = mediatorLiveData {

        ProcessLifecycleOwner.get().awaitResume()

        syncDataUseCase.execute().collect {

        }
    }


    val initCompleted = mutableSharedFlow<Boolean>()

    init {

        sync.asFlow().launchIn(viewModelScope)

        inputLanguageFlow.launchCollect(viewModelScope) {

            logAnalytics("input_language_code_${it?.id}")
        }

        outputLanguageFlow.launchCollect(viewModelScope) {

            logAnalytics("output_language_code_${it.id}")
        }
    }

    fun initCompleted() {

        initCompleted.tryEmit(true)
    }
}