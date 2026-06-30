package com.simple.phonetics.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simple.coreapp.utils.ext.handler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
fun <T1, R> ViewModel.combineState(
    flow1: Flow<T1>,
    initialValue: R,
    context: CoroutineContext = handler + Dispatchers.IO,
    transform: suspend MutableStateFlow<R>.(T1) -> Unit
): StateFlow<R> {
    val state = MutableStateFlow(initialValue)
    flow1.flatMapLatest { v1 ->
        flow<Unit> {
            state.transform(v1)
        }
    }
        .flowOn(context)
        .launchIn(viewModelScope)
    return state.asStateFlow()
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T1, T2, R> ViewModel.combineState(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    initialValue: R,
    context: CoroutineContext = handler + Dispatchers.IO,
    transform: suspend MutableStateFlow<R>.(T1, T2) -> Unit
): StateFlow<R> {
    val state = MutableStateFlow(initialValue)
    combine(flow1, flow2) { v1, v2 ->
        v1 to v2
    }
        .flatMapLatest { (v1, v2) ->
            flow<Unit> {
                state.transform(v1, v2)
            }
        }
        .flowOn(context)
        .launchIn(viewModelScope)
    return state.asStateFlow()
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T1, T2, T3, R> ViewModel.combineState(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    initialValue: R,
    context: CoroutineContext = handler + Dispatchers.IO,
    transform: suspend MutableStateFlow<R>.(T1, T2, T3) -> Unit
): StateFlow<R> {
    val state = MutableStateFlow(initialValue)
    combine(flow1, flow2, flow3) { v1, v2, v3 ->
        Triple(v1, v2, v3)
    }
        .flatMapLatest { (v1, v2, v3) ->
            flow<Unit> {
                state.transform(v1, v2, v3)
            }
        }
        .flowOn(context)
        .launchIn(viewModelScope)
    return state.asStateFlow()
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T1, T2, T3, T4, R> ViewModel.combineState(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    initialValue: R,
    context: CoroutineContext = handler + Dispatchers.IO,
    transform: suspend MutableStateFlow<R>.(T1, T2, T3, T4) -> Unit
): StateFlow<R> {
    val state = MutableStateFlow(initialValue)
    combine(flow1, flow2, flow3, flow4) { v1, v2, v3, v4 ->
        arrayOf(v1, v2, v3, v4)
    }.flatMapLatest { (v1, v2, v3, v4) ->
        flow<Unit> {
            @Suppress("UNCHECKED_CAST")
            state.transform(v1 as T1, v2 as T2, v3 as T3, v4 as T4)
        }
    }.flowOn(context).launchIn(viewModelScope)

    return state.asStateFlow()
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T1, T2, T3, T4, T5, R> ViewModel.combineState(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    initialValue: R,
    context: CoroutineContext = handler + Dispatchers.IO,
    transform: suspend MutableStateFlow<R>.(T1, T2, T3, T4, T5) -> Unit
): StateFlow<R> {
    val state = MutableStateFlow(initialValue)
    combine(flow1, flow2, flow3, flow4, flow5) { v1, v2, v3, v4, v5 ->
        arrayOf(v1, v2, v3, v4, v5)
    }
        .flatMapLatest { (v1, v2, v3, v4, v5) ->
            flow<Unit> {
                @Suppress("UNCHECKED_CAST")
                state.transform(v1 as T1, v2 as T2, v3 as T3, v4 as T4, v5 as T5)
            }
        }
        .flowOn(context)
        .launchIn(viewModelScope)
    return state.asStateFlow()
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T1, T2, T3, T4, T5, T6, R> ViewModel.combineState(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    initialValue: R,
    context: CoroutineContext = handler + Dispatchers.IO,
    transform: suspend MutableStateFlow<R>.(T1, T2, T3, T4, T5, T6) -> Unit
): StateFlow<R> {
    val state = MutableStateFlow(initialValue)
    combine(
        combine(flow1, flow2, flow3) { t1, t2, t3 -> Triple(t1, t2, t3) },
        combine(flow4, flow5, flow6) { t4, t5, t6 -> Triple(t4, t5, t6) }
    ) { t123, t456 ->
        t123 to t456
    }
        .flatMapLatest { (t123, t456) ->
            flow<Unit> {
                val (t1, t2, t3) = t123
                val (t4, t5, t6) = t456
                state.transform(t1, t2, t3, t4, t5, t6)
            }
        }
        .flowOn(context)
        .launchIn(viewModelScope)
    return state.asStateFlow()
}
