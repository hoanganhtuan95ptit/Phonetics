package com.simple.phonetics.ui.base.fragments

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableSharedFlow

enum class LifecycleState {

    ATTACH,

    CREATED,

    RESUMED,

    PAUSE,

    DESTROYED;

    companion object {

        fun LifecycleState.isCanBinding() = this in listOf(CREATED, RESUMED)
    }
}

interface LifecycleService {

    var stateFlow: MutableSharedFlow<LifecycleState>

    var lifecycleOwnerFlow: MutableSharedFlow<LifecycleOwner>
}

interface ViewLifecycleService {

    var viewStateFlow: MutableSharedFlow<LifecycleState>

    var viewLifecycleOwnerFlow: MutableSharedFlow<LifecycleOwner>
}