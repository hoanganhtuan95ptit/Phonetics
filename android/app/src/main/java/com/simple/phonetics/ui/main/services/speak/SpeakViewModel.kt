package com.simple.phonetics.ui.main.services.speak

import androidx.lifecycle.viewModelScope
import com.simple.phonetics.domain.repositories.SpeakRepository
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.unknown.coroutines.handler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpeakViewModel : BaseViewModel() {

    fun notifyInitCompleted() = viewModelScope.launch(handler+ Dispatchers.IO){

        SpeakRepository.instant.initCompleted()
    }
}