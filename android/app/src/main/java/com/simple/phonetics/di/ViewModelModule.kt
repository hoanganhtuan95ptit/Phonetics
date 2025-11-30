package com.simple.phonetics.di

import com.simple.coreapp.ui.base.fragments.transition.TransitionGlobalViewModel
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.MainViewModel
import com.simple.phonetics.ui.game.GameConfigViewModel
import com.simple.phonetics.ui.game.GameViewModel
import com.simple.phonetics.ui.game.congratulations.GameCongratulationViewModel
import com.simple.phonetics.ui.game.items.ipa_match.GameIPAMatchViewModel
import com.simple.phonetics.ui.game.items.ipa_puzzle.GameIPAPuzzleViewModel
import com.simple.phonetics.ui.game.items.ipa_wordle.GameIPAWordleViewModel
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.services.detect.DetectHomeViewModel
import com.simple.phonetics.ui.home.services.game.GameHomeServiceModel
import com.simple.phonetics.ui.home.services.history.HistoryHomeViewModel
import com.simple.phonetics.ui.home.services.ipa.IpaHomeViewModel
import com.simple.phonetics.ui.home.services.microphone.MicrophoneHomeViewModel
import com.simple.phonetics.ui.home.services.phonetic.PhoneticHomeViewModel
import com.simple.phonetics.ui.home.services.suggest.SuggestHomeViewModel
import com.simple.phonetics.ui.ipa.detail.IpaDetailViewModel
import com.simple.phonetics.ui.ipa.list.IpaListViewModel
import com.simple.phonetics.ui.language.LanguageViewModel
import com.simple.phonetics.ui.recording.RecordingViewModel
import com.simple.phonetics.ui.services.event.EventViewModel
import com.simple.phonetics.ui.services.review.ReviewViewModel
import com.simple.phonetics.ui.services.update.UpdateViewModel
import com.simple.phonetics.ui.speak.SpeakViewModel
import com.simple.phonetics.ui.services.ads.AdsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

@JvmField
val viewModelModule = module {

    viewModel {
        MainViewModel(get())
    }

    viewModel {
        TransitionGlobalViewModel()
    }

    viewModel {
        SuggestHomeViewModel(get())
    }

    viewModel {
        AdsViewModel(get())
    }

    viewModel {
        UpdateViewModel(get())
    }

    viewModel {
        LanguageViewModel(get(), get(), get())
    }


    viewModel {
        ConfigViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
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
        IpaDetailViewModel(get(), get(), get())
    }

    viewModel {
        EventViewModel(get(), get())
    }

    viewModel {
        ReviewViewModel(get())
    }

    viewModel {
        IpaHomeViewModel(get())
    }

    viewModel {
        HistoryHomeViewModel(get())
    }

    viewModel {
        GameHomeServiceModel(get())
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
        HomeViewModel(get(), get(), get(), get())
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
}