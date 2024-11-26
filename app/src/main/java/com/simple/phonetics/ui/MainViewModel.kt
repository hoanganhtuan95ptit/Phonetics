package com.simple.phonetics.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModels.BaseViewModel
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.domain.usecase.language.GetLanguageInputUseCase
import com.simple.phonetics.entities.Language

class MainViewModel(
    private val getLanguageInputUseCase: GetLanguageInputUseCase
) : BaseViewModel() {

    val languageInputLanguage: LiveData<Language?> = mediatorLiveData {

        Log.d("tuanha", "12345: ")
        runCatching {

            postValue(getLanguageInputUseCase.execute())
        }.getOrElse {

            Log.d("tuanha", "languageInputLanguage: ", it)
        }
    }
}