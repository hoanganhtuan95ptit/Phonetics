package com.simple.phonetics.ui.base.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.simple.coreapp.ui.base.fragments.transition.TransitionViewModel
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.domain.usecase.language.input.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.output.GetLanguageOutputAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.code.GetPhoneticCodeSelectedAsyncUseCase
import com.simple.phonetics.domain.usecase.reading.CheckSupportReadingAsyncUseCase
import com.simple.phonetics.domain.usecase.speak.CheckSupportSpeakAsyncUseCase
import com.simple.phonetics.entities.Language
import com.unknown.color.appColor
import com.unknown.size.appSize
import com.unknown.string.appString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import java.util.concurrent.ConcurrentHashMap

abstract class BaseViewModel : TransitionViewModel() {


    private val map = ConcurrentHashMap<String, Job>()


    val size: LiveData<Map<String, Int>> = mediatorLiveData {

        appSize.collect {

            postDifferentValue(it.toMutableMap())
        }
    }

    val theme: LiveData<Map<String, Int>> = mediatorLiveData {

        appColor.collect {

            postDifferentValue(it.toMutableMap())
        }
    }

    val translate: LiveData<Map<String, String>> = mediatorLiveData {

        appString.collect {

            postDifferentValue(it.toMutableMap())
        }
    }

    val inputLanguage: LiveData<Language> = mediatorLiveData {

        GlobalContext.get().get<GetLanguageInputAsyncUseCase>().execute().collect {

            postDifferentValue(it)
        }
    }

    val outputLanguage: LiveData<Language> = mediatorLiveData {

        GlobalContext.get().get<GetLanguageOutputAsyncUseCase>().execute().collect {

            postDifferentValue(it)
        }
    }


    open val isSupportSpeak: LiveData<Boolean> = mediatorLiveData {

        GlobalContext.get().get<CheckSupportSpeakAsyncUseCase>().execute().collect {

            postDifferentValue(it)
        }
    }

    val isSupportReading: LiveData<Boolean> = mediatorLiveData {

        GlobalContext.get().get<CheckSupportReadingAsyncUseCase>().execute().collect {

            postDifferentValue(it)
        }
    }


    val phoneticCodeSelected: LiveData<String> = mediatorLiveData {

        GlobalContext.get().get<GetPhoneticCodeSelectedAsyncUseCase>().execute().collect {

            postDifferentValue(it)
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