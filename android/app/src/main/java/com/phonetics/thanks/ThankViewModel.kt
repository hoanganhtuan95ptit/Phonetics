package com.phonetics.thanks

import androidx.lifecycle.LiveData
import com.phonetics.thanks.repositories.CommunityRepository
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.ui.base.fragments.BaseViewModel

class ThankViewModel : BaseViewModel() {

    val thank: LiveData<Map<String, Thank>> = mediatorLiveData {

        CommunityRepository.instance.getCommunitiesAsync().collect { list ->

            postDifferentValue(list.associateBy { it.id.orEmpty() })
        }
    }
}