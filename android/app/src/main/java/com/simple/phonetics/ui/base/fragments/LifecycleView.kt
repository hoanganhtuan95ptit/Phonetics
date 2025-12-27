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

        fun LifecycleState.doCreated(block: () -> Unit) {
            if (this == CREATED) block()
        }

        fun LifecycleState.doResumed(block: () -> Unit) {
            if (this == RESUMED) block()
        }

        fun LifecycleState.doPause(block: () -> Unit) {
            if (this == PAUSE) block()
        }

        fun LifecycleState.doDestroyed(block: () -> Unit) {
            if (this == DESTROYED) block()
        }
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