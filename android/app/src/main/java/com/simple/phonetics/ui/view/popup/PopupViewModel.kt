package com.simple.phonetics.ui.view.popup

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.simple.core.utils.extentions.orZero
import com.simple.coreapp.utils.JobQueue
import com.simple.coreapp.utils.extentions.postValue
import com.simple.coreapp.utils.extentions.toEvent
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.state.ResultState
import com.simple.state.isCompleted
import com.simple.state.toSuccess

class PopupViewModel : BaseViewModel() {

    @VisibleForTesting
    val job = JobQueue()

    @VisibleForTesting
    val popup: LiveData<MutableMap<String, ResultState<DeeplinkInfo>>> = MediatorLiveData(mutableMapOf())

    val popupEvent = popup.toEvent()


    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }

    fun addEvent(key: String, index: Int = 0, deepLink: String? = null, extras: Map<String, Any?>? = null) = job.submit {

        val map = popup.value ?: return@submit

        val state = if (deepLink == null) {

            ResultState.Start
        } else ResultState.Success(

            DeeplinkInfo(deepLink = deepLink, index = index, extras = extras)
        )

        map[key] = state

        if (map.any { !it.value.isCompleted() }) {

            return@submit
        }

        Log.d("tuanha", "addEvent: $key $deepLink")
        map.toList().sortedBy {

            it.second.toSuccess()?.data?.index.orZero()
        }.toMap().toMutableMap().let {

            popup.postValue(it)
        }
    }

    data class DeeplinkInfo(
        val deepLink: String,

        val index: Int,
        val extras: Map<String, Any?>?
    )
}