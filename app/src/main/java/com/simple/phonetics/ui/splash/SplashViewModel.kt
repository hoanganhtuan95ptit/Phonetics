package com.simple.phonetics.ui.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModels.BaseViewModel
import com.simple.coreapp.utils.extentions.listenerSources
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.domain.usecase.key_translate.GetKeyTranslateAsyncUseCase
import com.simple.phonetics.entities.Language
import com.simple.phonetics.ui.base.TransitionViewModel
import com.simple.state.ResultState

class SplashViewModel(
    private val getKeyTranslateAsyncUseCase: GetKeyTranslateAsyncUseCase
) : TransitionViewModel() {

    val languageState: LiveData<ResultState<List<Language>>> = MediatorLiveData()

    val keyTranslateMap: LiveData<Map<String, String>> = mediatorLiveData {

        getKeyTranslateAsyncUseCase.execute().collect {

            postDifferentValue(it)
        }
    }

    val screenInfo: LiveData<ScreenInfo> = listenerSources(languageState, keyTranslateMap) {

    }

    data class ScreenInfo(
        val message: String,
        val isLoading: Boolean
    )
}