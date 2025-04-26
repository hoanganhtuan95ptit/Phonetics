package com.simple.phonetics.utils

import androidx.lifecycle.LifecycleOwner
import com.simple.coreapp.utils.JobQueue
import com.simple.phonetics.utils.exts.awaitResume
import com.simple.state.ResultState
import com.simple.state.isCompleted
import com.simple.state.toSuccess
import kotlinx.coroutines.delay


private data class DeeplinkInfo(
    val deepLink: String,

    val index: Int,
    val extras: Map<String, Any?>?
)

private val jobQueue by lazy {

    JobQueue()
}

private val deepLinkMap by lazy {

    hashMapOf<String, ResultState<DeeplinkInfo>>()
}

fun sendDeeplink(key: String, lifecycleOwner: LifecycleOwner? = null, index: Int = 0, deepLink: String? = null, extras: Map<String, Any?>? = null) = jobQueue.submit {

    val map = deepLinkMap

    val state = if (deepLink == null) {

        ResultState.Start
    } else ResultState.Success(

        DeeplinkInfo(deepLink = deepLink, index = index, extras = extras)
    )

    map[key] = state

    if (map.any { !it.value.isCompleted() }) {

        return@submit
    }

    map.values.mapNotNull {

        it.toSuccess()?.data
    }.sortedBy {

        it.index
    }.forEach {

        lifecycleOwner?.awaitResume()

        com.simple.deeplink.sendDeeplink(extras = it.extras, deepLink = it.deepLink)

        delay(350)
    }
}