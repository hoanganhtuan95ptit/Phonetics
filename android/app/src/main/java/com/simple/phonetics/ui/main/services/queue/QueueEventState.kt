package com.simple.phonetics.ui.main.services.queue

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import com.simple.state.ResultState
import com.simple.state.isRunning
import com.simple.state.isStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import java.util.concurrent.ConcurrentHashMap

object QueueEventState {

    private val map = ConcurrentHashMap<String, Pair<Int, ResultState<Unit>>>()
    private val queueLive = MediatorLiveData<Long>()


    fun addTag(tag: String, order: Int = Int.MAX_VALUE) {

        updateState(tag = tag, order = order, state = ResultState.Start)
    }

    fun endTag(tag: String, order: Int = Int.MAX_VALUE, success: Boolean = true) {

        val state = if (success) {
            ResultState.Success(Unit)
        } else {
            ResultState.Failed(RuntimeException())
        }

        updateState(tag = tag, order = order, state = state)
    }

    fun updateState(tag: String, order: Int = Int.MAX_VALUE, state: ResultState<Unit>) {

        map[tag] = order to state
        queueLive.postValue(System.currentTimeMillis())
    }

    fun getQueueAsync(): Flow<String> = queueLive.asFlow().map {

        map.toList()
    }.filter {

        it.all { !it.second.second.isStart() }
    }.mapNotNull { it ->

        it.sortedBy { it.second.first }.firstOrNull { it.second.second.isRunning() }?.first
    }.distinctUntilChanged()
}