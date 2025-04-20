package com.simple.phonetics.di

import com.simple.coreapp.ui.base.fragments.transition.TransitionGlobalViewModel
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.MainViewModel
import com.simple.phonetics.ui.base.VerticalConfirmViewModel
import com.simple.phonetics.ui.game.GameConfigViewModel
import com.simple.phonetics.ui.game.GameViewModel
import com.simple.phonetics.ui.game.congratulations.GameCongratulationViewModel
import com.simple.phonetics.ui.game.items.ipa_match.GameIPAMatchViewModel
import com.simple.phonetics.ui.game.items.ipa_puzzle.GameIPAPuzzleViewModel
import com.simple.phonetics.ui.game.items.ipa_wordle.GameIPAWordleViewModel
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.view.detect.DetectHomeViewModel
import com.simple.phonetics.ui.home.view.event.EventHomeViewModel
import com.simple.phonetics.ui.home.view.game.GameHomeViewModel
import com.simple.phonetics.ui.home.view.history.HistoryHomeViewModel
import com.simple.phonetics.ui.home.view.ipa.IpaHomeViewModel
import com.simple.phonetics.ui.home.view.microphone.MicrophoneHomeViewModel
import com.simple.phonetics.ui.home.view.phonetic.PhoneticHomeViewModel
import com.simple.phonetics.ui.home.view.review.ReviewHomeViewModel
import com.simple.phonetics.ui.ipa.detail.IpaDetailViewModel
import com.simple.phonetics.ui.ipa.list.IpaListViewModel
import com.simple.phonetics.ui.language.LanguageViewModel
import com.simple.phonetics.ui.recording.RecordingViewModel
import com.simple.phonetics.ui.speak.SpeakViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

@JvmField
val viewModelModule = module {

    viewModel {
        MainViewModel(get(), get(), get(), get(), get())
    }

    viewModel {
        TransitionGlobalViewModel()
    }

    viewModel {
        ReviewHomeViewModel(get())
    }

    viewModel {
        LanguageViewModel(get(), get(), get())
    }


    viewModel {
        ConfigViewModel(get(), get())
    }


    viewModel {
        SpeakViewModel(get(), get(), get(), get(), get())
    }

    viewModel {
        RecordingViewModel(get(), get())
    }


    viewModel {
        IpaListViewModel(get())
    }

    viewModel {
        IpaDetailViewModel(get(), get(), get(), get(), get())
    }


    viewModel {
        EventHomeViewModel(get(), get())
    }

    viewModel {
        IpaHomeViewModel(get())
    }

    viewModel {
        HistoryHomeViewModel(get())
    }

    viewModel {
        GameHomeViewModel(get())
    }

    viewModel {
        DetectHomeViewModel(get())
    }

    viewModel {
        PhoneticHomeViewModel(get())
    }

    viewModel {
        MicrophoneHomeViewModel(get())
    }

    viewModel {
        HomeViewModel(get(), get(), get(), get(), get())
    }


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


    viewModel {
        VerticalConfirmViewModel()
    }
}