package com.simple.phonetics.utils.exts

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.withResumed
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first

suspend fun LifecycleOwner.awaitResume() = channelFlow {

    withResumed {
        trySend(Unit)
    }

    awaitClose {

    }
}.first()