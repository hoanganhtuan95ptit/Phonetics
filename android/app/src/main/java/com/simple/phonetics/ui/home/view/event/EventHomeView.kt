package com.simple.phonetics.ui.home.view.event

import android.content.ComponentCallbacks
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import com.simple.analytics.logAnalytics
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.deeplink.sendDeeplink
import com.simple.event.listenerEvent
import com.simple.phonetics.BuildConfig
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.ui.home.EventViewModel
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.utils.exts.awaitResume
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val tag = "EVENT_HOME"

interface EventHomeView {

    fun setupEvent(fragment: HomeFragment)

    fun showEvent()
}

class EventHomeViewImpl : EventHomeView {

    private val show: LiveData<Boolean> = MediatorLiveData()

    override fun setupEvent(fragment: HomeFragment) {

        val viewModel: EventHomeViewModel by fragment.viewModel()

        val eventViewModel: EventViewModel by fragment.viewModel()


        eventViewModel.addEvent(key = tag)

        viewModel.eventInfoEvent.asFlow().launchCollect(fragment.viewLifecycleOwner) { event ->

            show.asFlow().first()

            val info = event.getContentIfNotHandled() ?: return@launchCollect

            showEventAwait(fragment = fragment, info = info)

            viewModel.updateShowEvent()
        }
    }

    override fun showEvent() {

        show.postDifferentValue(true)
    }

    private suspend fun showEventAwait(fragment: HomeFragment, info: EventHomeViewModel.EventInfo) = channelFlow {

        val keyRequest = "EVENT_KEY_REQUEST"

        val eventViewModel: EventViewModel by fragment.viewModel()

        listenerEvent(keyRequest) {

            val binding = fragment.binding

            if (binding == null) {

                trySend(Unit)
                return@listenerEvent
            }

            val result = it.asObjectOrNull<Int>()

            val deeplink = if (result == 1) {
                if (BuildConfig.DEBUG) DeeplinkManager.IPA_LIST else info.event.positiveDeepLink
            } else {
                info.event.negativeDeepLink
            }

            val transitionName = binding.vTemp.transitionName

            sendDeeplink(
                deepLink = deeplink,
                extras = mapOf(
                    Param.ROOT_TRANSITION_NAME to transitionName
                ),
                sharedElement = mapOf(
                    transitionName to binding.vTemp
                )
            )

            logAnalytics(info.event.id + "_" + info.event.positiveDeepLink)

            trySend(Unit)
        }


        val extras = mapOf(
            com.simple.coreapp.Param.CANCEL to false,

            com.simple.coreapp.Param.POSITIVE to info.positive,
            com.simple.coreapp.Param.NEGATIVE to info.negative,

            com.simple.coreapp.Param.KEY_REQUEST to keyRequest,

            Param.VIEW_ITEM_LIST to info.viewItemList
        )

        eventViewModel.addEvent(
            key = tag,
            index = 1,
            deepLink = DeeplinkManager.EVENT,

            extras = extras
        )

        logAnalytics("event_show_${info.event.name.lowercase()}")

        awaitClose {

        }
    }.first()
}

@Deeplink(queue = "Confirm")
class EventDeeplinkHandler : DeeplinkHandler {

    override fun getDeeplink(): String {
        return DeeplinkManager.EVENT
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is HomeFragment) {
            return false
        }

        val extrasWrap = extras.orEmpty().toMutableMap()

        val keyRequest = extrasWrap[Param.KEY_REQUEST].asObjectOrNull<String>() ?: tag.apply {

            extrasWrap[Param.KEY_REQUEST] = this
        }


        componentCallbacks.awaitResume()


        sendDeeplink(DeeplinkManager.CONFIRM + "code:event", extras = extrasWrap)


        channelFlow {

            listenerEvent(eventName = keyRequest) {

                trySend(Unit)
            }

            awaitClose {

            }
        }.first()

        return true
    }
}