package com.phonetics.game

import android.content.Context
import androidx.startup.Initializer
import com.phonetics.game.ui.GameConfigViewModel
import com.phonetics.game.ui.GameViewModel
import com.phonetics.game.ui.congratulations.GameCongratulationViewModel
import com.phonetics.game.ui.items.ipa_match.GameIPAMatchViewModel
import com.phonetics.game.ui.items.ipa_puzzle.GameIPAPuzzleViewModel
import com.phonetics.game.ui.items.ipa_wordle.GameIPAWordleViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class GameInitializer : Initializer<Unit> {

    override fun create(context: Context) {

        loadKoinModules(
            module {

                viewModel {
                    GameViewModel()
                }

                viewModel {
                    GameConfigViewModel(get(), get())
                }

                viewModel {
                    GameCongratulationViewModel()
                }

                viewModel {
                    GameIPAPuzzleViewModel(get(), get())
                }

                viewModel {
                    GameIPAMatchViewModel(get(), get())
                }

                viewModel {
                    GameIPAWordleViewModel(get(), get(), get())
                }
            }
        )

        return
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
