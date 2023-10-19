package com.simple.phonetics.di

import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.phonetics.GetPhoneticsHistoryAsyncUseCase
import com.simple.phonetics.domain.usecase.SyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageInputAsyncUseCase
import com.simple.phonetics.domain.usecase.language.GetLanguageOutputAsyncUseCase
import org.koin.dsl.module

@JvmField
val useCaseModule = module {

    single {
        SyncUseCase(getAll())
    }

    single {
        GetPhoneticsAsyncUseCase(get(), get(), getAll())
    }

    single {
        GetPhoneticsHistoryAsyncUseCase(get())
    }

    single {
        GetLanguageInputAsyncUseCase(get())
    }

    single {
        GetLanguageOutputAsyncUseCase(get())
    }
}