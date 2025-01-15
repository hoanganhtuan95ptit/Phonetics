package com.simple.phonetics.utils.exts

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.simple.coreapp.utils.ext.handler
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


fun <T> LiveData<T>.launchCollect(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,

    start: CoroutineStart = CoroutineStart.DEFAULT,
    context: CoroutineContext = EmptyCoroutineContext,

    block: suspend (isFromCache: Boolean, data: T?) -> Unit
) {

    lifecycleOwner.lifecycleScope.launch(start = start, context = handler + context) {

        block(true, this@launchCollect.value)

        this@launchCollect.asFlow().collect {

            block(false, it)
        }
    }
}
