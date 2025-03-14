package com.simple.phonetics.di

import com.simple.phonetics.data.repositories.AppRepositoryImpl
import com.simple.phonetics.data.repositories.HistoryRepositoryImpl
import com.simple.phonetics.data.repositories.IpaRepositoryImpl
import com.simple.phonetics.data.repositories.LanguageRepositoryImpl
import com.simple.phonetics.data.repositories.ListenRepositoryImpl
import com.simple.phonetics.data.repositories.PhoneticRepositoryImpl
import com.simple.phonetics.data.repositories.WordRepositoryImpl
import com.simple.phonetics.data.repositories.SpeakRepositoryImpl
import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.repositories.HistoryRepository
import com.simple.phonetics.domain.repositories.IpaRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.ListenRepository
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.domain.repositories.WordRepository
import com.simple.phonetics.domain.repositories.SpeakRepository
import org.koin.dsl.module

@JvmField
val repositoryModule = module {

    single<AppRepository> { AppRepositoryImpl(get(), get(), get(), getAll()) }

    single<IpaRepository> { IpaRepositoryImpl(get(), get()) }

    single<SpeakRepository> { SpeakRepositoryImpl() }

    single<ListenRepository> { ListenRepositoryImpl() }

    single<PhoneticRepository> { PhoneticRepositoryImpl(get(), get()) }

    single<LanguageRepository> { LanguageRepositoryImpl(get(), get()) }

    single<HistoryRepository> { HistoryRepositoryImpl(get()) }

    single<WordRepository> { WordRepositoryImpl(get(), get()) }
}