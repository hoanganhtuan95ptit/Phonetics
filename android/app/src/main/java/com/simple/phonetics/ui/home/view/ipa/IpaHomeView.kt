package com.simple.phonetics.ui.home.view.ipa

import android.view.View
import androidx.lifecycle.asFlow
import com.google.auto.service.AutoService
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.EventName
import com.simple.phonetics.Id
import com.simple.phonetics.Param
import com.simple.phonetics.ui.base.adapters.IpaViewItem
import com.simple.phonetics.ui.base.adapters.TextSimpleViewItem
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.view.HomeView
import org.koin.androidx.viewmodel.ext.android.viewModel

@AutoService(HomeView::class)
class IpaHomeView : HomeView {

    override fun setup(fragment: HomeFragment) {

        val viewModel: HomeViewModel by fragment.viewModel()

        val ipaHomeViewModel: IpaHomeViewModel by fragment.viewModel()

        ipaHomeViewModel.ipaViewItemList.asFlow().launchCollect(fragment.viewLifecycleOwner) {

            viewModel.updateTypeViewItemList(type = 2, it)
        }


        com.simple.event.listenerEvent(eventName = EventName.TEXT_SIMPLE_VIEW_ITEM_CLICKED, lifecycle = fragment.viewLifecycleOwner.lifecycle) {

            val (view, viewItem) = it.asObjectOrNull<Pair<View, TextSimpleViewItem>>() ?: return@listenerEvent

            if (!viewItem.id.startsWith(Id.IPA_LIST)) {

                return@listenerEvent
            }

            val transitionName = view.transitionName ?: viewItem.id

            sendDeeplink(
                deepLink = DeeplinkManager.IPA_LIST,
                extras = mapOf(Param.ROOT_TRANSITION_NAME to transitionName),
                sharedElement = mapOf(transitionName to view)
            )
        }

        com.simple.event.listenerEvent(eventName = EventName.IPA_VIEW_ITEM_CLICKED, lifecycle = fragment.viewLifecycleOwner.lifecycle) {

            val (view, viewItem) = it.asObjectOrNull<Pair<View, IpaViewItem>>() ?: return@listenerEvent

            val transitionName = view.transitionName ?: viewItem.id

            sendDeeplink(
                deepLink = DeeplinkManager.IPA_DETAIL,
                extras = mapOf(
                    Param.IPA to viewItem.data,
                    Param.ROOT_TRANSITION_NAME to transitionName
                ),
                sharedElement = mapOf(
                    transitionName to view
                )
            )
        }
    }
}