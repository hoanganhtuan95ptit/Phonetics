package com.simple.phonetics.ui.base.services.transition

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

inline fun LifecycleOwner.launchWithResumed(crossinline block: () -> Unit) = lifecycleScope.launch {

    withResumed(block)
}

fun Fragment.doObserver(lifecycleObserver: LifecycleObserver) = channelFlow<Unit> {

    lifecycle.addObserver(lifecycleObserver)

    awaitClose {
        lifecycle.removeObserver(lifecycleObserver)
    }
}.launchIn(this.lifecycleScope)