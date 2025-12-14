package com.simple.phonetics.ui.main.services.event

import android.view.View
import androidx.lifecycle.asFlow
import com.simple.analytics.logAnalytics
import com.simple.autobind.annotation.AutoBind
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.deeplink.sendDeeplink
import com.simple.event.listenerEvent
import com.simple.phonetics.BuildConfig
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.R
import com.simple.phonetics.ui.main.MainActivity
import com.simple.phonetics.ui.main.services.MainService
import com.simple.phonetics.ui.main.services.queue.QueueEventState
import com.simple.state.ResultState
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val tag = "EVENT_HOME"
private const val order = 8
private const val KEY_REQUEST = "${tag}_KEY_REQUEST"

@AutoBind(MainActivity::class)
class EventService : MainService {

    override fun setup(mainActivity: MainActivity) {

        val viewModel: EventViewModel by mainActivity.viewModel()

        QueueEventState.addTag(tag = tag, order = order)

        viewModel.eventInfo.asFlow().launchCollect(mainActivity) { data ->

            val state = if (data.show) {
                ResultState.Running(Unit)
            } else {
                ResultState.Success(Unit)
            }

            QueueEventState.updateState(tag = tag, order = order, state = state)
        }

        listenerEvent(mainActivity.lifecycle, tag) {

            val info = viewModel.eventInfo.asFlow().first()

            if (info.show) {

                showEventAwait(mainActivity = mainActivity, info = info)
                viewModel.updateShowEvent()

                delay(350)
            }


            QueueEventState.endTag(tag = tag, order = order)
        }
    }

    private suspend fun showEventAwait(mainActivity: MainActivity, info: EventViewModel.EventInfo) = channelFlow {

        val event = info.event ?: return@channelFlow

        listenerEvent(KEY_REQUEST) {

            val result = it.asObjectOrNull<Int>()

            val deeplink = if (result == 1) {
                if (BuildConfig.DEBUG) DeeplinkManager.IPA_LIST else event.positiveDeepLink
            } else {
                event.negativeDeepLink
            }

            val viewTemp = mainActivity.findViewById<View>(R.id.v_temp)
            val transitionName = viewTemp.transitionName

            sendDeeplink(
                deepLink = deeplink,
                extras = mapOf(
                    Param.ROOT_TRANSITION_NAME to transitionName
                ),
                sharedElement = mapOf(
                    transitionName to viewTemp
                )
            )

            logAnalytics(event.id + "_" + event.positiveDeepLink)

            trySend(result)
        }


        val extras = mapOf(
            com.simple.coreapp.Param.CANCEL to false,

            com.simple.coreapp.Param.POSITIVE to info.positive,
            com.simple.coreapp.Param.NEGATIVE to info.negative,

            com.simple.coreapp.Param.KEY_REQUEST to KEY_REQUEST,

            Param.VIEW_ITEM_LIST to info.viewItemList
        )

        sendDeeplink(DeeplinkManager.CONFIRM + "code:event", extras = extras)

        logAnalytics("event_show_${event.name.lowercase()}")

        awaitClose {

        }
    }.first()
}