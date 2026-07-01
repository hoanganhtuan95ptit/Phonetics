package com.simple.phonetics.ui.base.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.coreapp.utils.extentions.postValue
import kotlinx.coroutines.flow.MutableStateFlow

open class BaseActionViewModel : BaseViewModel() {

    @Deprecated("Use actionHeightFlow")
    val actionHeight: LiveData<Int> = MediatorLiveData()

    val actionHeightFlow: MutableStateFlow<Int> = MutableStateFlow(0)

    fun updateActionHeight(height: Int) {

        actionHeight.postValue(height)
        actionHeightFlow.value = height
    }
}