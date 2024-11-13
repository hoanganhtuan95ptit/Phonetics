package com.simple.phonetics.di

import com.simple.phonetics.ui.MainViewModel
import com.simple.phonetics.ui.phonetics.PhoneticsViewModel
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.language.LanguageFragment
import com.simple.phonetics.ui.language.LanguageViewModel
import com.simple.phonetics.ui.splash.SplashViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

@JvmField
val viewModelModule = module {

    viewModel {
        MainViewModel(get(), get())
    }

    viewModel {
        SplashViewModel(get())
    }

    viewModel {
        LanguageViewModel(get(), get(), get(), get())
    }

    viewModel {
        ConfigViewModel(get(), get(), get(), get(), get())
    }

    viewModel {
        PhoneticsViewModel(get(), get(), get(), get(), get(), get())
    }
}