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


fun sendDeeplink(deepLink: String, data: Map<String, Any>, sharedElement: Map<String, View>) {

    sendDeeplink(deepLink, bundleOf(*data.toList().toTypedArray()), sharedElement)
}

fun sendDeeplink(deepLink: String, extras: Bundle? = null, sharedElement: Map<String, View>? = null) = CoroutineScope(Dispatchers.Main.immediate).launch {

    if (!appDeeplink.replayCache.toMap().containsKey(deepLink)) {

        appDeeplink.emit(deepLink to (extras to sharedElement))
    }
}


interface NavigationView {

    fun setupNavigation(activity: ComponentActivity)
}

class NavigationViewImpl : NavigationView {

    override fun setupNavigation(activity: ComponentActivity) {

        var job: Job? = null

        activity.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {

            job?.cancel()

            job = appDeeplink.launchCollect(activity) {

                val deepLink = it.first

                val extras = it.second.first
                val sharedElement = it.second.second

                val navigation = withContext(Dispatchers.IO) {

                    list.find { it.acceptDeeplink(deepLink) }
                }

                if (navigation?.navigation(activity, deepLink, extras, sharedElement) == true) {

                    appDeeplink.resetReplayCache()
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
