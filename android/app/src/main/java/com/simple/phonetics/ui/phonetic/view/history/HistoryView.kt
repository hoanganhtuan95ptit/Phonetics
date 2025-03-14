package com.simple.phonetics.ui.phonetic.view.history

import com.simple.phonetics.ui.phonetic.PhoneticsFragment
import com.simple.phonetics.ui.phonetic.PhoneticViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

interface HistoryView {

    fun setupHistory(fragment: PhoneticsFragment)
}

class HistoryViewImpl : HistoryView {

    override fun setupHistory(fragment: PhoneticsFragment) {

        val viewModel: PhoneticViewModel by fragment.viewModel()

        val historyViewModel: HistoryViewModel by fragment.viewModel()

        historyViewModel.historyViewItemList.observe(fragment.viewLifecycleOwner) {

            viewModel.updateTypeViewItemList(type = Int.MAX_VALUE, it)
        }
    }
}