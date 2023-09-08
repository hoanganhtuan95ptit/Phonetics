package com.simple.phonetics.di

import com.simple.phonetics.domain.usecase.GetPhoneticsAsyncUseCase
import com.simple.phonetics.domain.usecase.GetPhoneticsHistoryAsyncUseCase
import com.simple.phonetics.domain.usecase.SyncUseCase
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
}