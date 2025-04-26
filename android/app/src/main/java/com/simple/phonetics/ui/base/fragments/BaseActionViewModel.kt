package com.simple.phonetics.ui.base.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.coreapp.utils.extentions.postDifferentValue

open class BaseActionViewModel : BaseViewModel() {

    val actionHeight: LiveData<Int> = MediatorLiveData()

    fun updateActionHeight(height: Int) {

        actionHeight.postDifferentValue(height)
    }
}