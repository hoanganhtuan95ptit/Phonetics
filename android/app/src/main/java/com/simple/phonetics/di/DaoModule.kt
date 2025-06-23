package com.simple.phonetics.di

import com.simple.phonetics.data.dao.PhoneticRoomDatabaseProvider
import com.simple.phonetics.data.dao.ipa.IpaOldProvider
import com.simple.phonetics.data.dao.translate.TranslateProvider
import com.simple.phonetics.data.dao.word.WordOldProvider
import org.koin.dsl.module

@JvmField
val daoModule = module {

    single {
        PhoneticRoomDatabaseProvider(context = get())
    }

    single {
        get<PhoneticRoomDatabaseProvider>().historyDao
    }

    single {
        get<PhoneticRoomDatabaseProvider>().phoneticDao
    }

    single {
        get<PhoneticRoomDatabaseProvider>().keyTranslateDao
    }

    single {
        IpaOldProvider(get())
    }

    single {
        get<IpaOldProvider>().ipaDao
    }

    single {
        WordOldProvider(get())
    }

    single {
        get<WordOldProvider>().wordDao
    }

    single {
        TranslateProvider(get())
    }

    single {
        get<TranslateProvider>().translateDao
    }
}