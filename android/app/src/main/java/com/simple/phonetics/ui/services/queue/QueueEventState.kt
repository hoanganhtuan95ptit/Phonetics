package com.simple.phonetics.ui.services.queue

import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import com.simple.state.ResultState
import com.simple.state.isRunning
import com.simple.state.isStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import java.util.concurrent.ConcurrentHashMap

object QueueEventState {

    private val map = ConcurrentHashMap<String, ResultState<Unit>>()
    private val queueLive = MediatorLiveData<Long>()


    fun addTag(tag: String) {

        updateState(tag = tag, state = ResultState.Start)
    }

    fun endTag(tag: String, success: Boolean = true) {

        val state = if (success) {
            ResultState.Success(Unit)
        } else {
            ResultState.Failed(RuntimeException())
        }

        updateState(tag = tag, state = state)
    }

    fun updateState(tag: String, state: ResultState<Unit>) {

        map[tag] = state
        queueLive.postValue(System.currentTimeMillis())
    }

    fun getQueueAsync(): Flow<String> = queueLive.asFlow().map {

        map.toList()
    }.filter {

        Log.d("tuanha", "getQueueAsync: $it")
        it.all { !it.second.isStart() }
    }.mapNotNull { it ->

        it.firstOrNull { it.second.isRunning() }?.first
    }
}