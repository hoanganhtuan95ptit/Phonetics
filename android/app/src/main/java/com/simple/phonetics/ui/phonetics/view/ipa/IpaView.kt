package com.simple.phonetics.ui.phonetics.view.ipa

import com.simple.phonetics.ui.phonetics.PhoneticsFragment
import com.simple.phonetics.ui.phonetics.PhoneticsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

interface IpaView {

    fun setupIpa(fragment: PhoneticsFragment)
}

class IpaViewImpl : IpaView {

    override fun setupIpa(fragment: PhoneticsFragment) {

        val viewModel: PhoneticsViewModel by fragment.viewModel()

        val ipaViewModel: IpaViewModel by fragment.viewModel()

        ipaViewModel.ipaViewItemList.observe(fragment.viewLifecycleOwner) {

            viewModel.updateIpaViewItemList(it)
        }
    }
}