package com.simple.phonetics.ui.base

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModels.BaseViewModel

abstract class TransitionViewModel : BaseViewModel() {

    val transitionEnterEnd: MediatorLiveData<Boolean> = MediatorLiveData()

    val transitionEnd: MediatorLiveData<Boolean> = MediatorLiveData()
}