package com.simple.phonetics.ui.base

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModels.BaseViewModel
import androidx.lifecycle.asFlow
import com.simple.state.ResultState
import com.simple.state.isSuccess
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

abstract class TransitionViewModel : BaseViewModel() {

    val transition: MediatorLiveData<Map<String, ResultState<*>>> = MediatorLiveData()

    fun transitionState(tag: String, state: ResultState<*>) {

        val map = transition.value?.toMutableMap() ?: HashMap()

        map[tag] = state

        transition.postValue(map)
    }

    suspend fun awaitTransition() {

        transition.asFlow().filter { map ->
            map.values.all { it.isSuccess() }
        }.first()
    }

}