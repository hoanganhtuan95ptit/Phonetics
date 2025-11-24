package com.simple.phonetics.ui.base.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.simple.coreapp.ui.base.fragments.transition.TransitionViewModel
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.phonetics.domain.usecase.language.input.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.output.GetLanguageOutputAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.code.GetPhoneticCodeSelectedAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.CheckSupportReadingAsyncUseCase
import com.simple.phonetics.domain.usecase.speak.CheckSupportSpeakAsyncUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.utils.TextViewMetrics
import com.simple.phonetics.utils.appStyle
import com.simple.phonetics.utils.exts.mutableSharedFlowWithDiff
import com.unknown.size.appSize
import com.unknown.string.appString
import com.unknown.theme.appTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import java.util.concurrent.ConcurrentHashMap

abstract class BaseViewModel : TransitionViewModel() {


    private val map = ConcurrentHashMap<String, Job>()


    val size: LiveData<Map<String, Int>> = mediatorLiveData {

        appSize.collect {

            postValue(it.toMutableMap())
        }
    }

    val style: LiveData<Map<String, TextViewMetrics>> = mediatorLiveData {

        appStyle.collect {

            postValue(it)
        }
    }

    val theme: LiveData<Map<String, Any>> = mediatorLiveData {

        appTheme.collect {

            postValue(it.toMutableMap())
        }
    }

    val themeFlow = mutableSharedFlowWithDiff {

        appTheme.collect {
            emit(it.toMap())
        }
    }

    val translate: LiveData<Map<String, String>> = mediatorLiveData {

        appString.collect {

            postValue(it.toMutableMap())
        }
    }

    val translateFlow = mutableSharedFlowWithDiff {

        appString.collect {
            emit(it.toMap())
        }
    }

    val inputLanguage: LiveData<Language> = mediatorLiveData {

        GlobalContext.get().get<GetLanguageInputAsyncUseCase>().execute().collect {

            postValue(it)
        }
    }

    val inputLanguageFlow = mutableSharedFlowWithDiff {

        GlobalContext.get().get<GetLanguageInputAsyncUseCase>().execute().collect {

            emit(it)
        }
    }

    val outputLanguage: LiveData<Language> = mediatorLiveData {

        GlobalContext.get().get<GetLanguageOutputAsyncUseCase>().execute().collect {

            postValue(it)
        }
    }

    val outputLanguageFlow = mutableSharedFlowWithDiff {

        GlobalContext.get().get<GetLanguageOutputAsyncUseCase>().execute().collect {

            emit(it)
        }
    }


    open val isSupportSpeak: LiveData<Boolean> = mediatorLiveData {

        postValue(false)

        GlobalContext.get().get<CheckSupportSpeakAsyncUseCase>().execute().collect {

            postValue(it)
        }
    }

    val isSupportReading: LiveData<Boolean> = mediatorLiveData {

        postValue(false)

        GlobalContext.get().get<CheckSupportReadingAsyncUseCase>().execute().collect {

            postValue(it)
        }
    }

    val isSupportReadingFlow: Flow<Boolean> = mutableSharedFlowWithDiff {

        emit(false)

        GlobalContext.get().get<CheckSupportReadingAsyncUseCase>().execute().collect {

            emit(it)
        }
    }


    val phoneticCodeSelected: LiveData<String> = mediatorLiveData {

        GlobalContext.get().get<GetPhoneticCodeSelectedAsyncUseCase>().execute().collect {

            postValue(it)
        }
    }

    override fun onCleared() {
        super.onCleared()
        map.values.clear()
        map.clear()
    }

    fun launchWithTag(tag: String, block: suspend CoroutineScope.() -> Unit) {

        map[tag]?.cancel()
        map[tag] = viewModelScope.launch(handler + Dispatchers.IO, block = block)
    }
}