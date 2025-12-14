package com.simple.phonetics.ui.home.services.history

import android.view.View
import com.simple.autobind.annotation.AutoBind
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.event.listenerEvent
import com.simple.phonetics.EventName
import com.simple.phonetics.TYPE_HISTORY
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.adapters.HistoryViewItem
import com.simple.phonetics.ui.home.services.HomeService
import org.koin.androidx.viewmodel.ext.android.viewModel

@AutoBind(HomeFragment::class)
class HistoryHomeService : HomeService {

    override fun setup(homeFragment: HomeFragment) {

        val homeViewModel: HomeViewModel by homeFragment.viewModel()

        val viewModel: HistoryHomeViewModel by homeFragment.viewModel()

        viewModel.viewItemList.observe(homeFragment.viewLifecycleOwner) {

            homeViewModel.updateTypeViewItemList(type = TYPE_HISTORY, it)
        }


        listenerEvent(eventName = EventName.HISTORY_VIEW_ITEM_CLICKED, lifecycle = homeFragment.viewLifecycleOwner.lifecycle) {

            val (_, viewItem) = it.asObjectOrNull<Pair<View, HistoryViewItem>>() ?: return@listenerEvent

            homeViewModel.getPhonetics("")
            homeFragment.binding?.etText?.setText(viewItem.id)
        }
    }
}