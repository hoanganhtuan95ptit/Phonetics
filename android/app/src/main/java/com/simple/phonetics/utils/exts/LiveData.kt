package com.simple.phonetics.utils.exts

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
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

fun <T> LiveData<T>.launchCollectWithCache(
    lifecycleOwner: LifecycleOwner,

    start: CoroutineStart = CoroutineStart.DEFAULT,
    context: CoroutineContext = EmptyCoroutineContext,

    collector: suspend (data: T?, isFirst: Boolean) -> Unit
) = lifecycleOwner.lifecycleScope.launch(start = start, context = context) {

    val data = value

    collector(data, true)

    asFlow().collect {

        collector(it, false)
    }
}