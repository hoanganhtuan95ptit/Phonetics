package com.simple.phonetics.utils.exts

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.simple.coreapp.utils.ext.handler
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


fun <T> LiveData<T>.launchCollect(
    lifecycleOwner: LifecycleOwner,

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

    collector: suspend (data: T, isFirst: Boolean) -> Unit
) = lifecycleOwner.lifecycleScope.launch(start = start, context = context) {

    var data = value

    if (data != null) collector(data, true)

    asFlow().collect {

        val diff = data == null || withContext(context + Dispatchers.IO) {
            data != it
        }

        if (diff) collector(it, false)

        data = null
    }
}