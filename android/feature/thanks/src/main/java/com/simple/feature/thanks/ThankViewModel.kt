package com.simple.feature.thanks

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.feature.thanks.data.repositories.CommunityRepository
import com.simple.feature.thanks.entities.Thank
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ThankViewModel : BaseViewModel() {

    val thank: LiveData<Map<String, Thank>> = mediatorLiveData {

        CommunityRepository.Companion.instance.getCommunitiesAsync().collect { list ->

            postValue(list.associateBy { it.id.orEmpty() })
        }
    }

    fun sendThank(deepLink: String) = viewModelScope.launch(handler + Dispatchers.IO) {

        CommunityRepository.Companion.instance.updateSendThank(deepLink)
    }
}