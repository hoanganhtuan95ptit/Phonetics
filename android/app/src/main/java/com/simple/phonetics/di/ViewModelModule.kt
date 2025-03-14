package com.simple.phonetics.di

import com.simple.coreapp.ui.base.fragments.transition.TransitionGlobalViewModel
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.MainViewModel
import com.simple.phonetics.ui.game.GameViewModel
import com.simple.phonetics.ui.game.GameConfigViewModel
import com.simple.phonetics.ui.game.ipa_wordle.GameIPAWordleViewModel
import com.simple.phonetics.ui.ipa.detail.IpaDetailViewModel
import com.simple.phonetics.ui.ipa.list.IpaListViewModel
import com.simple.phonetics.ui.language.LanguageViewModel
import com.simple.phonetics.ui.phonetic.PhoneticViewModel
import com.simple.phonetics.ui.phonetic.view.detect.DetectViewModel
import com.simple.phonetics.ui.phonetic.view.game.GameHomeViewModel
import com.simple.phonetics.ui.phonetic.view.history.HistoryViewModel
import com.simple.phonetics.ui.phonetic.view.ipa.IpaViewModel
import com.simple.phonetics.ui.phonetic.view.microphone.MicrophoneViewModel
import com.simple.phonetics.ui.phonetic.view.review.AppReviewViewModel
import com.simple.phonetics.ui.recording.RecordingViewModel
import com.simple.phonetics.ui.speak.SpeakViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

@JvmField
val viewModelModule = module {

    viewModel {
        MainViewModel(get(), get())
    }

    viewModel {
        TransitionGlobalViewModel()
    }

    viewModel {
        AppReviewViewModel(get())
    }

    viewModel {
        LanguageViewModel(get(), get(), get())
    }

    viewModel {
        ConfigViewModel(get(), get(), get(), get())
    }


    viewModel {
        SpeakViewModel(get(), get(), get(), get(), get(), get(), get())
    }

    viewModel {
        RecordingViewModel(get(), get(), get(), get())
    }


    viewModel {
        IpaListViewModel(get())
    }

    viewModel {
        IpaDetailViewModel(get(), get(), get(), get(), get())
    }


    viewModel {
        IpaViewModel(get(), get())
    }

    viewModel {
        HistoryViewModel(get())
    }

    viewModel {
        GameHomeViewModel(get(), get())
    }

    viewModel {
        DetectViewModel(get(), get(), get())
    }

    viewModel {
        MicrophoneViewModel(get(), get(), get())
    }

    viewModel {
        PhoneticViewModel(get(), get(), get(), get(), get())
    }


    viewModel {
        GameViewModel()
    }

    viewModel {
        GameConfigViewModel(get(), get())
    }

    viewModel {
        GameIPAWordleViewModel(get(), get(), get(), get())
    }
}