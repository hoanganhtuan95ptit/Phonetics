package com.simple.phonetics.ui.phonetics.view.history

import com.simple.phonetics.ui.phonetics.PhoneticsFragment
import com.simple.phonetics.ui.phonetics.PhoneticsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

interface HistoryView {

    fun setupHistory(fragment: PhoneticsFragment)
}

class HistoryViewImpl : HistoryView {

    override fun setupHistory(fragment: PhoneticsFragment) {

        val viewModel: PhoneticsViewModel by fragment.viewModel()

        val historyViewModel: HistoryViewModel by fragment.viewModel()

        historyViewModel.historyViewItemList.observe(fragment.viewLifecycleOwner) {

            viewModel.updateHistoryViewItemList(it)
        }
    }
}