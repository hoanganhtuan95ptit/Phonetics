package com.simple.phonetics.ui.home.view.game

import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

interface GameHomeView {

    fun setupGame(fragment: HomeFragment)
}

class GameHomeViewImpl : GameHomeView {

    override fun setupGame(fragment: HomeFragment) {

        val viewModel: HomeViewModel by fragment.viewModel()

        val gameHomeViewModel: GameHomeViewModel by fragment.viewModel()

        gameHomeViewModel.viewItemList.observe(fragment.viewLifecycleOwner) {

            viewModel.updateTypeViewItemList(type = 0, it)
        }
    }
}