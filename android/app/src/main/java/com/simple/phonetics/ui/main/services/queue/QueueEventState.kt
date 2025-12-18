package com.simple.phonetics.ui.main.services.queue

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import java.util.concurrent.ConcurrentHashMap

object QueueEventState {


    private val queueMap = MutableLiveData(ConcurrentHashMap<String, QueueEvent>())


    fun addTag(tag: String, order: Int = Int.MAX_VALUE) {

        updateState(tag = tag, order = order, status = Status.None)
    }

    fun readyTag(tag: String) {

        updateState(tag = tag, status = Status.Ready)
    }

    fun runningTag(tag: String) {

        updateState(tag = tag, status = Status.Running)
    }

    fun endTag(tag: String) {

        updateState(tag = tag, status = Status.End)
    }


    fun updateState(tag: String, order: Int = Int.MAX_VALUE, status: Status) {

        val map = queueMap.value ?: return

        map[tag] = map[tag]?.copy(status = status) ?: QueueEvent(tag = tag, order = order, status = status)

        queueMap.postValue(map)
    }

    fun getQueueAsync(): Flow<String> = queueMap.asFlow().filter {

        it.all { it.value.status !in listOf(Status.None, Status.Running) }
    }.mapNotNull { it ->

        it.values.sortedBy { it.order }.firstOrNull { it.status == Status.Ready }?.tag
    }.distinctUntilChanged()


    private data class QueueEvent(
        val tag: String,
        val order: Int,
        val status: Status
    )

    enum class Status {
        None,
        Ready,
        Running,
        End
    }
}