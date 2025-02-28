package com.simple.phonetics.di

import androidx.room.Room
import com.simple.phonetics.data.dao.IpaRoomDatabase
import com.simple.phonetics.data.dao.PhoneticsRoomDatabase
import org.koin.dsl.module

@JvmField
val daoModule = module {

    single {
        Room.databaseBuilder(get(), PhoneticsRoomDatabase::class.java, "phonetics_database")
            .build()
    }

    single {
        get<PhoneticsRoomDatabase>().providerKeyTranslateDao()
    }

    single {
        get<PhoneticsRoomDatabase>().providerPhoneticsDao()
    }

    single {
        get<PhoneticsRoomDatabase>().providerHistoryDao()
    }

    single {
        Room.databaseBuilder(get(), IpaRoomDatabase::class.java, "ipa_database")
            .build().providerIpaDao()
    }
}