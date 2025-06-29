package com.phonetics.thank

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.phonetics.thank.data.repositories.CommunityRepository
import com.phonetics.thank.entities.Thank
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ThankViewModel : BaseViewModel() {

    val thank: LiveData<Map<String, Thank>> = mediatorLiveData {

        CommunityRepository.instance.getCommunitiesAsync().collect { list ->

            postValue(list.associateBy { it.id.orEmpty() })
        }
    }

    fun sendThank(deepLink: String) = viewModelScope.launch(handler + Dispatchers.IO) {

        CommunityRepository.instance.updateSendThank(deepLink)
    }
}