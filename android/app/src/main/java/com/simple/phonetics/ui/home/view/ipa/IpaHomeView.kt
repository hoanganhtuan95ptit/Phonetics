package com.simple.phonetics.ui.home.view.ipa

import android.view.View
import com.google.auto.service.AutoService
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.ui.adapters.texts.ClickTextViewItem
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.EventName
import com.simple.phonetics.Id
import com.simple.phonetics.Param
import com.simple.phonetics.ui.base.adapters.IpaViewItem
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.view.HomeView
import org.koin.androidx.viewmodel.ext.android.viewModel

@AutoService(HomeView::class)
class IpaHomeView : HomeView {

    override fun setup(fragment: HomeFragment) {

        val viewModel: HomeViewModel by fragment.viewModel()

        val ipaHomeViewModel: IpaHomeViewModel by fragment.viewModel()

        ipaHomeViewModel.ipaViewItemList.observe(fragment.viewLifecycleOwner) {

            viewModel.updateTypeViewItemList(type = 2, it)
        }


        com.simple.event.listenerEvent(eventName = com.simple.coreapp.EventName.TEXT_VIEW_ITEM_CLICKED, lifecycle = fragment.viewLifecycleOwner.lifecycle) {

            val (view, viewItem) = it.asObjectOrNull<Pair<View, ClickTextViewItem>>() ?: return@listenerEvent

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