package com.simple.phonetics.utils.exts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn

fun CoroutineScope.doOnClose(close: () -> Unit) {

    channelFlow<Unit> {

        awaitClose {

            close.invoke()
        }
    }.launchIn(this)
}