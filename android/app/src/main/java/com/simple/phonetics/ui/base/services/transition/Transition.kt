package com.simple.phonetics.ui.base.services.transition

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import kotlinx.coroutines.launch

inline fun LifecycleOwner.launchWithResumed(crossinline block: () -> Unit) = lifecycleScope.launch {

    withResumed(block)
}