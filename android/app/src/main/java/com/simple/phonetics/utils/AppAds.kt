package com.simple.phonetics.utils

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow


val appAds by lazy {

    MutableSharedFlow<Long>(replay = 0, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
}

fun showAds() {

    appAds.tryEmit(System.currentTimeMillis())
}
