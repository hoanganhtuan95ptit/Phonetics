package com.simple.phonetics.ui.base.services.transition.locking

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

class LockingTransitionViewModel : ViewModel() {

    var locking = MutableLiveData(ConcurrentHashMap<String, Locking>())

    val lockingFilter = locking.asFlow().filter { it.isNotEmpty() }


    val isUnlock = lockingFilter.map { map -> map.all { !it.value.isLocking } }

    val lockingData = lockingFilter.map { list -> list.filter { it.value.isLocking } }.distinctUntilChanged().map {

        LockInfo(
            data = it,
            isStart = it.isEmpty()
        )
    }

    init {

        isUnlock.launchIn(viewModelScope)
        lockingData.launchIn(viewModelScope)
    }

    fun lockTransition(tag: String) {

        updateTransitionLock(tag = tag, isLocking = true)
    }

    fun unlockTransition(tag: String) {

        updateTransitionLock(tag = tag, isLocking = false)
    }

    private fun updateTransitionLock(tag: String, isLocking: Boolean) {

        val map = locking.value ?: return

        map[tag] = map[tag]?.copy(isLocking = isLocking) ?: Locking(tag = tag, isLocking = isLocking)

        locking.value = map
    }

    data class Locking(
        val tag: String,
        val isLocking: Boolean,
        val timeAdd: Long = System.currentTimeMillis()
    ) {
        val timeLocking: Long
            get() = System.currentTimeMillis() - timeAdd
    }

    data class LockInfo(
        val data: Map<String, Locking>,
        val isStart: Boolean
    )
}
