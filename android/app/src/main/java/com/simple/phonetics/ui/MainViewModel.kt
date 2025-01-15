package com.simple.phonetics.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModels.BaseViewModel
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.phonetics.domain.usecase.language.GetLanguageInputUseCase
import com.simple.phonetics.entities.Language

class MainViewModel(
    private val getLanguageInputUseCase: GetLanguageInputUseCase
) : BaseViewModel() {

    val languageInputLanguage: LiveData<Language?> = mediatorLiveData {

        postValue(getLanguageInputUseCase.execute())
    }
}