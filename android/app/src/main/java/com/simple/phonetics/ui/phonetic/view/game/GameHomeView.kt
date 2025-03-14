package com.simple.phonetics.ui.phonetic.view.game

import com.simple.phonetics.ui.phonetic.PhoneticsFragment
import com.simple.phonetics.ui.phonetic.PhoneticViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

interface GameView {

    fun setupGame(fragment: PhoneticsFragment)
}

class GameViewImpl : GameView {

    override fun setupGame(fragment: PhoneticsFragment) {

        val viewModel: PhoneticViewModel by fragment.viewModel()

        val gameHomeViewModel: GameHomeViewModel by fragment.viewModel()

        gameHomeViewModel.viewItemList.observe(fragment.viewLifecycleOwner) {

            viewModel.updateTypeViewItemList(type = 0, it)
        }
    }
}