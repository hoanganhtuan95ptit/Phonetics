package com.simple.phonetics.ui.main.services.block

import androidx.lifecycle.LiveData
import com.simple.coreapp.utils.extentions.combineSourcesWithDiff
import com.simple.coreapp.utils.extentions.mediatorLiveData
import com.simple.phonetics.BuildConfig
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.AppNew
import com.simple.phonetics.utils.exts.fromBase64

class BlockViewModel : BaseViewModel() {

    val isNew: LiveData<Boolean> = mediatorLiveData {

        postValue(AppNew.isNew(key = "block", timeout = 3 * 24 * 60 * 60 * 1000))
    }

    val isAcceptApp: LiveData<Boolean> = mediatorLiveData {

        val list = arrayListOf("Y29tLmlwYS5lbmdsaXNoLnBob25ldGljcw==".fromBase64(), "Y29tLmlwYS5lbmdsaXNoLnBob25ldGljcy5kZWJ1Zw==".fromBase64())

        val accept = list.any { it.equals(BuildConfig.APPLICATION_ID, true) }

        postValue(accept)
    }

    val info: LiveData<BlockInfo> = combineSourcesWithDiff(isNew, isAcceptApp) {


    }

    data class BlockInfo(
        val isShow: Boolean,

        val title: String,
        val message: String
    )
}