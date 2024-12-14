package com.simple.phonetics.di

import com.simple.phonetics.data.repositories.AppRepositoryImpl
import com.simple.phonetics.data.repositories.LanguageRepositoryImpl
import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import org.koin.dsl.module

@JvmField
val repositoryModule = module {

    single<AppRepository> {
        AppRepositoryImpl(get(), get())
    }

    single<LanguageRepository> {
        LanguageRepositoryImpl(get(), get(), get(), getAll())
    }
}