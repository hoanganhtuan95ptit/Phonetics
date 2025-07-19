package com.simple.phonetics.ui

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModels.BaseViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.phonetics.word.dao.WordProvider
import com.simple.analytics.logAnalytics
import com.simple.core.utils.extentions.toJson
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.phonetics.BuildConfig
import com.simple.phonetics.data.dao.PhoneticRoomDatabaseProvider
import com.simple.phonetics.domain.usecase.SyncDataUseCase
import com.simple.phonetics.domain.usecase.language.input.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.input.GetLanguageInputUseCase
import com.simple.phonetics.domain.usecase.language.output.GetLanguageOutputAsyncUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.utils.exts.awaitResume
import com.simple.state.ResultState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class MainViewModel(
    private val syncDataUseCase: SyncDataUseCase,
    private val getLanguageInputUseCase: GetLanguageInputUseCase,
    private val getLanguageInputAsyncUseCase: GetLanguageInputAsyncUseCase,
    private val getLanguageOutputAsyncUseCase: GetLanguageOutputAsyncUseCase
) : BaseViewModel() {

    @VisibleForTesting
    val sync: LiveData<ResultState<Unit>> = mediatorLiveData {

        ProcessLifecycleOwner.get().awaitResume()
        syncDataUseCase.execute().collect {

        }
    }

    @VisibleForTesting
    val inputLanguage: LiveData<Language> = mediatorLiveData {

        getLanguageInputAsyncUseCase.execute().collect {

            logAnalytics("input_language_code_${it.id}")
        }
    }

    @VisibleForTesting
    val outputLanguage: LiveData<Language> = mediatorLiveData {

        getLanguageOutputAsyncUseCase.execute().collect {

            logAnalytics("output_language_code_${it.id}")
        }
    }

    val languageInputLanguage: LiveData<Language?> = mediatorLiveData {

        postValue(getLanguageInputUseCase.execute())
    }

    init {

        sync.asFlow().launchIn(viewModelScope)

        inputLanguage.asFlow().launchIn(viewModelScope)
        outputLanguage.asFlow().launchIn(viewModelScope)

        viewModelScope.launch(handler + Dispatchers.IO) {

            logAnalytics("version_name_${BuildConfig.VERSION_NAME.replace(".", "_")}")
        }

        if (BuildConfig.DEBUG) viewModelScope.launch(handler + Dispatchers.IO) {

            val phoneticDao = GlobalContext.get().get<PhoneticRoomDatabaseProvider>().phoneticDao

            GlobalContext.get().get<WordProvider>().wordDao.getRoomAll().forEach {

                val list = phoneticDao.getListBy(listOf(it.text.lowercase()))

                if (list.isEmpty() || list.first().ipa.size <= 1) Log.d("check empty", "resource:${it.resource} :::: ${it.text} ==> ${list.map { it.ipa }.toJson()}")
            }
        }
    }
}