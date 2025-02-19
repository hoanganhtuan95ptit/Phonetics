package com.simple.phonetics.utils

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.phonetics.Deeplink
import com.tuanha.deeplink.DeeplinkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface DeeplinkHandler {

    fun getDeeplink(): String = ""

    fun acceptDeeplink(deepLink: String): Boolean = deepLink == getDeeplink()

    suspend fun navigation(activity: ComponentActivity, deepLink: String, extras: Bundle?, sharedElement: Map<String, View>?): Boolean
}


private val list: List<DeeplinkHandler> by lazy {

    DeeplinkManager.navigation().filterIsInstance<DeeplinkHandler>()
}


private val appDeeplink by lazy {

    MutableSharedFlow<Pair<String, Pair<Bundle?, Map<String, View>?>>>(replay = 1, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
}

private val toastDeeplink by lazy {

    MutableSharedFlow<Pair<String, Pair<Bundle?, Map<String, View>?>>>(replay = 1, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
}

private val confirmDeeplink by lazy {

    MutableSharedFlow<Pair<String, Pair<Bundle?, Map<String, View>?>>>(replay = 1, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
}


fun sendToast(data: Map<String, Any>) {

    sendToast(extras = bundleOf(*data.toList().toTypedArray()))
}

fun sendToast(extras: Bundle? = null) {

    sendDeeplink(deepLink = Deeplink.TOAST, extras = extras)
}

fun sendConfirm(data: Map<String, Any>) {

    sendConfirm(extras = bundleOf(*data.toList().toTypedArray()))
}

fun sendConfirm(extras: Bundle? = null) {

    sendDeeplink(deepLink = Deeplink.CONFIRM, extras = extras)
}

fun sendDeeplink(deepLink: String, data: Map<String, Any>, sharedElement: Map<String, View>) {

    sendDeeplink(deepLink, bundleOf(*data.toList().toTypedArray()), sharedElement)
}

fun sendDeeplink(deepLink: String, extras: Bundle? = null, sharedElement: Map<String, View>? = null) = CoroutineScope(Dispatchers.Main.immediate).launch {

    val deepLinkFlow = if (deepLink.startsWith(Deeplink.TOAST)) {
        toastDeeplink
    } else if (deepLink.startsWith(Deeplink.CONFIRM)) {
        confirmDeeplink
    } else {
        appDeeplink
    }

    if (deepLinkFlow != appDeeplink || !deepLinkFlow.replayCache.toMap().containsKey(deepLink)) {

        deepLinkFlow.emit(deepLink to (extras to sharedElement))
    }
}


interface DeeplinkView {

    fun setupDeeplink(activity: ComponentActivity)
}

class DeeplinkViewImpl : DeeplinkView {

    override fun setupDeeplink(activity: ComponentActivity) {

        setupDeepLink(activity, appDeeplink)
        setupDeepLink(activity, toastDeeplink)
        setupDeepLink(activity, confirmDeeplink)
    }

    private fun setupDeepLink(activity: ComponentActivity, deeplinkFlow: MutableSharedFlow<Pair<String, Pair<Bundle?, Map<String, View>?>>>) {

        var job: Job? = null

        activity.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {

            job?.cancel()

            job = deeplinkFlow.launchCollect(activity) {

                val deepLink = it.first

                val extras = it.second.first
                val sharedElement = it.second.second

                val navigation = withContext(Dispatchers.IO) {

                    list.find { it.acceptDeeplink(deepLink) }
                }

                if (navigation?.navigation(activity, deepLink, extras, sharedElement) == true) {

                    deeplinkFlow.resetReplayCache()
                }
            }
        }
    }

    private fun LifecycleOwner.launchRepeatOnLifecycle(
        state: Lifecycle.State,
        block: suspend CoroutineScope.() -> Unit
    ) = lifecycleScope.launch {

        lifecycle.repeatOnLifecycle(state, block)
    }
}
