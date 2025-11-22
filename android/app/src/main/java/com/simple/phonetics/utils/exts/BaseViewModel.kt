package com.simple.phonetics.utils.exts

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.coreapp.utils.ext.handler
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@MainThread
fun <T> ViewModel.mutableSharedFlow(context: CoroutineContext? = null, start: CoroutineStart = CoroutineStart.DEFAULT, onChanged: suspend MutableSharedFlow<T>.() -> Unit): MutableSharedFlow<T> {

    val liveData = MutableSharedFlow<T>(replay = 1, extraBufferCapacity = 1)

    viewModelScope.launch(context = context ?: (handler + Dispatchers.IO), start = start) {
        onChanged.invoke(liveData)
    }

    return liveData
}

@MainThread
fun <T> ViewModel.combineSources(
    vararg sources: Flow<*>,
    context: CoroutineContext? = null,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    onChanged: suspend MutableSharedFlow<T>.() -> Unit
): MutableSharedFlow<T> = mutableSharedFlow(context = context, start = start) {

    var job: Job? = null

    combine(flows = sources) {}.collect {

        job?.cancel()
        job = viewModelScope.launch(context = context ?: (handler + Dispatchers.IO), start = start) {

            onChanged.invoke(this@mutableSharedFlow)
        }
    }
}

@MainThread
fun <T> ViewModel.listenerSources(
    vararg sources: Flow<*>,
    context: CoroutineContext? = null,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    onChanged: suspend MutableSharedFlow<T>.() -> Unit
): MutableSharedFlow<T> = mutableSharedFlow(context = context, start = start) {

    var job: Job? = null

    merge(flows = sources).collect {

        job?.cancel()
        job = viewModelScope.launch(context = context ?: (handler + Dispatchers.IO), start = start) {

            onChanged.invoke(this@mutableSharedFlow)
        }
    }
}

@MainThread
fun <T> ViewModel.mutableSharedFlowWithDiff(
    context: CoroutineContext? = null,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    onChanged: suspend MutableSharedFlow<T>.() -> Unit
): MutableSharedFlow<T> = MutableSharedFlow<T>().apply {

    var old: T? = null

    mutableSharedFlow(context, start, onChanged).launchCollect(viewModelScope) {

        if (old == it) return@launchCollect
        old = it

        emit(it)
    }
}

@MainThread
fun <T> ViewModel.combineSourcesWithDiff(
    vararg sources: Flow<*>,
    context: CoroutineContext? = null,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    onChanged: suspend MutableSharedFlow<T>.() -> Unit
): MutableSharedFlow<T> = MutableSharedFlow<T>().apply {

    var old: T? = null

    combineSources(sources = sources, context, start, onChanged).launchCollect(viewModelScope) {

        if (old == it) return@launchCollect
        old = it

        emit(it)
    }
}

@MainThread
fun <T> androidx.lifecycle.ViewModels.BaseViewModel.listenerSourcesWithDiff(
    vararg sources: Flow<*>,
    context: CoroutineContext? = null,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    onChanged: suspend MutableSharedFlow<T>.() -> Unit
): MutableSharedFlow<T> = MutableSharedFlow<T>().apply {

    var old: T? = null

    listenerSources(sources = sources, context, start, onChanged).launchCollect(viewModelScope) {

        if (old == it) return@launchCollect
        old = it

        emit(it)
    }
}

val <T> Flow<T>.value: T?
    get() = if (this is SharedFlow) replayCache.firstOrNull()
    else null


fun <T> Flow<T>.get(): T = value!!