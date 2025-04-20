package com.simple.phonetics.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.coroutineContext

@Deprecated("")
fun sendEvent(eventName: String, data: Any) {

    com.simple.event.sendEvent(eventName = eventName, data = data)
}

@Deprecated("")
suspend fun listenerEvent(eventName: String, block: suspend (data: Any) -> Unit) {

    listenerEvent(coroutineScope = CoroutineScope(coroutineContext), eventName = eventName, block = block)
}

@Deprecated("")
fun listenerEvent(lifecycle: Lifecycle, eventName: String, block: suspend (data: Any) -> Unit) {

    listenerEvent(coroutineScope = lifecycle.coroutineScope, eventName = eventName, block = block)
}

@Deprecated("")
fun listenerEvent(coroutineScope: CoroutineScope, eventName: String, block: suspend (data: Any) -> Unit) {

    com.simple.event.listenerEvent(coroutineScope = coroutineScope, eventName = eventName, block = block)
}

