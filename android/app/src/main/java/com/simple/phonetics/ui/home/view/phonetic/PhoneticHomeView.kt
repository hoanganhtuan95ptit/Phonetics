package com.simple.phonetics.ui.home.view.phonetic

import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

interface PhoneticHomeView {

    fun setupPhonetic(fragment: HomeFragment)
}

class PhoneticHomeViewImpl : PhoneticHomeView {

    override fun setupPhonetic(fragment: HomeFragment) {

        val viewModel: HomeViewModel by fragment.viewModel()

        val phoneticHomeViewModel: PhoneticHomeViewModel by fragment.viewModel()

        phoneticHomeViewModel.viewItemList.observe(fragment.viewLifecycleOwner) {

            viewModel.updateTypeViewItemList(type = -1, it)
        }
    }
}