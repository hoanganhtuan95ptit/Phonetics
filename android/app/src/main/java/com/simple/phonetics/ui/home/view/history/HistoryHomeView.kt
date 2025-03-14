package com.simple.phonetics.ui.home.view.history

import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
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
    }
}