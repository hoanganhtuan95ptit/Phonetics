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
import com.simple.phonetics.entities.Language
import com.simple.phonetics.utils.AppSize
import com.simple.phonetics.utils.AppTheme
import com.simple.phonetics.utils.appSize
import com.simple.phonetics.utils.appTheme
import com.simple.phonetics.utils.appTranslate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import java.util.concurrent.ConcurrentHashMap

abstract class BaseViewModel : TransitionViewModel() {


    private val map = ConcurrentHashMap<String, Job>()


    val size: LiveData<AppSize> = mediatorLiveData {

        appSize.collect {

            postDifferentValue(it)
        }
    }

    val theme: LiveData<AppTheme> = mediatorLiveData {

        appTheme.collect {

            postDifferentValue(it)
        }
    }

    val translate: LiveData<Map<String, String>> = mediatorLiveData {

        appTranslate.collect {

            postDifferentValue(it)
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