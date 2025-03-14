package com.simple.phonetics.ui.phonetic.view.ipa

import com.simple.phonetics.ui.phonetic.PhoneticsFragment
import com.simple.phonetics.ui.phonetic.PhoneticViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

interface IpaView {

    fun setupIpa(fragment: PhoneticsFragment)
}

class IpaViewImpl : IpaView {

    override fun setupIpa(fragment: PhoneticsFragment) {

        val viewModel: PhoneticViewModel by fragment.viewModel()

        val ipaViewModel: IpaViewModel by fragment.viewModel()

        ipaViewModel.ipaViewItemList.observe(fragment.viewLifecycleOwner) {

            viewModel.updateTypeViewItemList(type = 2, it)
        }
    }
}