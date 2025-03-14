package com.simple.phonetics.ui.home.view.ipa

import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

interface IpaHomeView {

    fun setupIpa(fragment: HomeFragment)
}

class IpaHomeViewImpl : IpaHomeView {

    override fun setupIpa(fragment: HomeFragment) {

        val viewModel: HomeViewModel by fragment.viewModel()

        val ipaHomeViewModel: IpaHomeViewModel by fragment.viewModel()

        ipaHomeViewModel.ipaViewItemList.observe(fragment.viewLifecycleOwner) {

            viewModel.updateTypeViewItemList(type = 2, it)
        }
    }
}