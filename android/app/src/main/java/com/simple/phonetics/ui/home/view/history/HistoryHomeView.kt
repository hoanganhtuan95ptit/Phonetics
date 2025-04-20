package com.simple.phonetics.ui.home.view.history

import android.view.View
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.phonetics.EventName
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.adapters.HistoryViewItem
import com.simple.phonetics.utils.showAds
import org.koin.androidx.viewmodel.ext.android.viewModel

interface HistoryHomeView {

    fun setupHistory(fragment: HomeFragment)
}

class HistoryHomeViewImpl : HistoryHomeView {

    override fun setupHistory(fragment: HomeFragment) {

        val viewModel: HomeViewModel by fragment.viewModel()

        val historyHomeViewModel: HistoryHomeViewModel by fragment.viewModel()

        historyHomeViewModel.historyViewItemList.observe(fragment.viewLifecycleOwner) {

            viewModel.updateTypeViewItemList(type = Int.MAX_VALUE, it)
        }


        com.simple.event.listenerEvent(eventName = EventName.HISTORY_VIEW_ITEM_CLICKED, lifecycle = fragment.viewLifecycleOwner.lifecycle) {

            val (_, viewItem) = it.asObjectOrNull<Pair<View, HistoryViewItem>>() ?: return@listenerEvent

            viewModel.getPhonetics("")
            fragment.binding?.etText?.setText(viewItem.id)

            showAds()
        }
    }
}