package com.simple.phonetics.ui.base.services.transition.running

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

class RunTransitionViewModel : ViewModel() {


    @VisibleForTesting
    val running = MutableLiveData(ConcurrentHashMap<String, Running>())

    val runningFilter = running.asFlow().filter { it.isNotEmpty() }


    val isRunEnd = runningFilter.map { list -> list.all { !it.value.isRunning } }

    val lockingData = runningFilter.map { list ->

        list.filter { it.value.isRunning }
    }.distinctUntilChanged().map {

        RunInfo(
            data = it,
            isRunning = it.isEmpty()
        )
    }

    init {

        isRunEnd.launchIn(viewModelScope)
        lockingData.launchIn(viewModelScope)
    }

    fun endTransition(tag: String) {

        updateTransitionRunning(tag = tag, isRunning = false)
    }

    fun startTransition(tag: String) {

        updateTransitionRunning(tag = tag, isRunning = true)
    }

    fun updateTransitionRunning(tag: String, isRunning: Boolean) {

        val map = running.value ?: return

        map[tag] = map[tag]?.copy(isRunning = isRunning) ?: Running(tag = tag, isRunning = isRunning)

        running.value = map
    }

    suspend fun onTransitionRunningEndAwait() {

        isRunEnd.filter { it }.first()
    }

    data class Running(
        val tag: String,
        val isRunning: Boolean,
        val timeAdd: Long = System.currentTimeMillis()
    )

    data class RunInfo(
        val data: Map<String, Running>,
        val isRunning: Boolean
    )
}
