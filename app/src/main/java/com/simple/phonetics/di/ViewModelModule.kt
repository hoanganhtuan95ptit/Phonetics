package com.simple.phonetics.di

import com.simple.phonetics.ui.MainViewModel
import com.simple.phonetics.ui.phonetics.PhoneticsViewModel
import com.simple.phonetics.ui.phonetics.config.PhoneticsConfigViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

@JvmField
val viewModelModule = module {

    viewModel {
        MainViewModel(get(), get())
    }

    viewModel {
        PhoneticsViewModel(get(), get(), get())
    }

    viewModel {
        PhoneticsConfigViewModel(get())
    }
}