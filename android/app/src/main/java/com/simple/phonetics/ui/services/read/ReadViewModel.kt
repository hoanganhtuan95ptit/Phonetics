package com.simple.phonetics.ui.services.read

import androidx.lifecycle.viewModelScope
import com.simple.phonetics.domain.repositories.ReadingRepository
import com.simple.phonetics.domain.repositories.SpeakRepository
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.unknown.coroutines.handler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReadViewModel: BaseViewModel() {

    fun notifyInitCompleted() = viewModelScope.launch(handler+ Dispatchers.IO){

        ReadingRepository.instant.initCompleted()
    }
}