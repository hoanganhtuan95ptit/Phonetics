package com.simple.phonetics.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.simple.coreapp.utils.ext.launchCollect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.coroutines.coroutineContext


private val appEvent by lazy {

    MutableSharedFlow<Pair<String, Any>>(replay = 0, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
}

fun sendEvent(eventName: String, data: Any) {

    appEvent.tryEmit(eventName to data)
}

suspend fun listenerEvent(eventName: String, block: suspend (data: Any) -> Unit) {

    listenerEvent(coroutineScope = CoroutineScope(coroutineContext), eventName = eventName, block = block)
}

fun listenerEvent(lifecycle: Lifecycle, eventName: String, block: suspend (data: Any) -> Unit) {

    listenerEvent(coroutineScope = lifecycle.coroutineScope, eventName = eventName, block = block)
}

fun listenerEvent(coroutineScope: CoroutineScope, eventName: String, block: suspend (data: Any) -> Unit) {

    appEvent.launchCollect(coroutineScope = coroutineScope) {

        if (it.first == eventName) block(it.second)
    }
}

