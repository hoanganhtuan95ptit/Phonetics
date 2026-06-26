package com.simple.phonetics.ui.home.services.game

import android.view.View
import com.simple.autobind.annotation.AutoBind
import com.simple.core.utils.extentions.asObject
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.EventName
import com.simple.coreapp.ui.adapters.texts.ClickTextViewItem
import com.simple.deeplink.sendDeeplink
import com.simple.event.listenerEvent
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.EventName.TEXT_SIMPLE_VIEW_ITEM_CLICKED
import com.simple.phonetics.Id
import com.simple.phonetics.Param
import com.simple.phonetics.ui.base.adapters.TextSimpleViewItem
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.services.HomeService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import org.koin.androidx.viewmodel.ext.android.viewModel

@AutoBind(HomeFragment::class)
class GameHomeService : HomeService {

    override fun setup(homeFragment: HomeFragment) {

        val homeViewModel: HomeViewModel by homeFragment.viewModel()

        val gameHomeServiceModel: GameHomeServiceModel by homeFragment.viewModel()

        gameHomeServiceModel.viewItemList.observe(homeFragment.viewLifecycleOwner) {

            homeViewModel.updateTypeViewItemList(type = 0, it)
        }

        listenerEvent(eventName = EventName.TEXT_VIEW_ITEM_CLICKED, lifecycle = homeFragment.viewLifecycleOwner.lifecycle) {

            val (view, viewItem) = it.asObjectOrNull<Pair<View, ClickTextViewItem>>() ?: return@listenerEvent

            if (!viewItem.id.startsWith(Id.GAME)) {

                return@listenerEvent
            }

            val result = openGameConfirmAsync().first()

            if (result == 1) {

                openGame(view = view, id = viewItem.id)
            }
        }

        listenerEvent(eventName = TEXT_SIMPLE_VIEW_ITEM_CLICKED, lifecycle = homeFragment.viewLifecycleOwner.lifecycle) {

            val (view, viewItem) = it.asObjectOrNull<Pair<View, TextSimpleViewItem>>() ?: return@listenerEvent

            if (!viewItem.id.startsWith(Id.GAME)) {

                return@listenerEvent
            }

            val result = openGameConfirmAsync().first()

            if (result == 1) {

                openGame(view = view, id = viewItem.id)
            }
        }
    }

    private fun openGameConfirmAsync() = channelFlow {

        val keyRequest = DeeplinkManager.GAME_CONFIG

        sendDeeplink(
            deepLink = DeeplinkManager.GAME_CONFIG,
            extras = mapOf(
                Param.KEY_REQUEST to keyRequest
            )
        )

        listenerEvent(coroutineScope = this, eventName = keyRequest) {

            trySend(it.asObject<Int>())
        }

        awaitClose {

        }
    }

    private fun openGame(view: View, id: String) {

        val transitionName = view.transitionName ?: id

        sendDeeplink(
            deepLink = DeeplinkManager.GAME,
            extras = mapOf(Param.ROOT_TRANSITION_NAME to transitionName),
            sharedElement = mapOf(transitionName to view)
        )
    }
}