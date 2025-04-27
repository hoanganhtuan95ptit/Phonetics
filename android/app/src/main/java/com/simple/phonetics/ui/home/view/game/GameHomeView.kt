package com.simple.phonetics.ui.home.view.game

import android.view.View
import com.google.auto.service.AutoService
import com.simple.core.utils.extentions.asObject
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.EventName
import com.simple.coreapp.ui.adapters.texts.ClickTextViewItem
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Id
import com.simple.phonetics.Param
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.view.HomeView
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import org.koin.androidx.viewmodel.ext.android.viewModel

@AutoService(HomeView::class)
class GameHomeView : HomeView {

    override fun setup(fragment: HomeFragment) {

        val viewModel: HomeViewModel by fragment.viewModel()

        val gameHomeViewModel: GameHomeViewModel by fragment.viewModel()

        gameHomeViewModel.viewItemList.observe(fragment.viewLifecycleOwner) {

            viewModel.updateTypeViewItemList(type = 0, it)
        }

        com.simple.event.listenerEvent(eventName = EventName.TEXT_VIEW_ITEM_CLICKED, lifecycle = fragment.viewLifecycleOwner.lifecycle) {

            val (view, viewItem) = it.asObjectOrNull<Pair<View, ClickTextViewItem>>() ?: return@listenerEvent

            if (!viewItem.id.startsWith(Id.GAME)) {

                return@listenerEvent
            }

            val result = openGameConfirmAwait().first()

            if (result == 1) {

                openGame(view = view, item = viewItem)
            }
        }
    }

    private fun openGameConfirmAwait() = channelFlow {

        val keyRequest = DeeplinkManager.GAME_CONFIG

        sendDeeplink(
            deepLink = DeeplinkManager.GAME_CONFIG,
            extras = mapOf(
                Param.KEY_REQUEST to keyRequest
            )
        )

        com.simple.event.listenerEvent(coroutineScope = this, eventName = keyRequest) {

            trySend(it.asObject<Int>())
        }

        awaitClose {

        }
    }

    private fun openGame(view: View, item: ClickTextViewItem) {

        val transitionName = view.transitionName ?: item.id

        sendDeeplink(
            deepLink = DeeplinkManager.GAME,
            extras = mapOf(Param.ROOT_TRANSITION_NAME to transitionName),
            sharedElement = mapOf(transitionName to view)
        )
    }
}