package com.simple.phonetics.utils

import com.simple.phonetics.entities.Language
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow


val appOutputLanguage by lazy {
    MutableSharedFlow<Language>(replay = 1, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
}

val appInputLanguage by lazy {
    MutableSharedFlow<Language>(replay = 1, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
}

val appPhoneticCodeSelected by lazy {
    MutableSharedFlow<String>(replay = 1, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
}
