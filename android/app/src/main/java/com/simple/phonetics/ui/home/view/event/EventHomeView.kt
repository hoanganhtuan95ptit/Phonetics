package com.simple.phonetics.ui.home.view.event

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import com.simple.analytics.logAnalytics
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.utils.listenerEvent
import com.simple.deeplink.sendDeeplink
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import org.koin.androidx.viewmodel.ext.android.viewModel


interface EventHomeView {

    fun setupEvent(fragment: HomeFragment)

    fun showEvent()
}

class EventHomeViewImpl : EventHomeView {

    private val show: LiveData<Boolean> = MediatorLiveData()

    override fun setupEvent(fragment: HomeFragment) {

        val viewModel: EventHomeViewModel by fragment.viewModel()

        viewModel.eventInfoEvent.asFlow().launchCollect(fragment.viewLifecycleOwner) { event ->

            val info = event.getContentIfNotHandled() ?: return@launchCollect

            showEventAwait(info = info)

            viewModel.updateShowEvent()
        }

        show.observe(fragment.viewLifecycleOwner) {

            viewModel.show()
        }
    }

    override fun showEvent() {

        show.postDifferentValue(true)
    }

    private suspend fun showEventAwait(info: EventHomeViewModel.EventInfo) = channelFlow {

        val keyRequest = "EVENT_KEY_REQUEST"

        listenerEvent(keyRequest) {

            val result = it.asObjectOrNull<Int>()

            if (result == 1) {

                sendDeeplink(info.event.positiveDeepLink)
            } else {

                sendDeeplink(info.event.negativeDeepLink)
            }

            trySend(Unit)
        }

        val extras = mapOf(
            com.simple.coreapp.Param.CANCEL to false,
            com.simple.coreapp.Param.IMAGE to info.image,

            com.simple.coreapp.Param.TITLE to info.title,
            com.simple.coreapp.Param.MESSAGE to info.message,

            com.simple.coreapp.Param.POSITIVE to info.positive,
            com.simple.coreapp.Param.NEGATIVE to info.negative,

            com.simple.coreapp.Param.ANCHOR to info.anchor,
            com.simple.coreapp.Param.BACKGROUND to info.background,

            com.simple.coreapp.Param.KEY_REQUEST to keyRequest
        )

        sendDeeplink(deepLink = DeeplinkManager.EVENT, extras = extras)

        logAnalytics("event_show_${info.event.name.lowercase()}")

        awaitClose {

        }
    }.first()
}