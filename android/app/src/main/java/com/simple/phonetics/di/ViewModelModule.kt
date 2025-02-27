package com.simple.phonetics.di

import com.simple.coreapp.ui.base.fragments.transition.TransitionGlobalViewModel
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.MainViewModel
import com.simple.phonetics.ui.ipa_detail.IpaDetailViewModel
import com.simple.phonetics.ui.ipa_list.IpaListViewModel
import com.simple.phonetics.ui.language.LanguageViewModel
import com.simple.phonetics.ui.phonetics.PhoneticsViewModel
import com.simple.phonetics.ui.phonetics.view.history.HistoryViewModel
import com.simple.phonetics.ui.phonetics.view.ipa.IpaViewModel
import com.simple.phonetics.ui.phonetics.view.review.AppReviewViewModel
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
        IpaViewModel(get())
    }

    viewModel {
        IpaListViewModel(get())
    }

    viewModel {
        IpaDetailViewModel(get(), get(), get(), get(), get())
    }


    viewModel {
        HistoryViewModel(get())
    }


    viewModel {
        PhoneticsViewModel(get(), get(), get(), get(), get(), get())
    }
}