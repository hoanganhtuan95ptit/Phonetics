package com.simple.phonetics.di

import androidx.room.Room
import com.simple.phonetics.data.dao.PhoneticRoomDatabase
import com.simple.phonetics.data.dao.PhoneticRoomDatabaseConstants
import com.simple.phonetics.data.dao.ipa.IpaRoomDatabase
import com.simple.phonetics.data.dao.translate.TranslateRoomDatabase
import com.simple.phonetics.data.dao.word.WordRoomDatabase
import org.koin.dsl.module

@JvmField
val daoModule = module {

    single {
        PhoneticRoomDatabaseConstants.instant(context = get())
    }

    single {
        get<PhoneticRoomDatabase>().providerHistoryDao()
    }

    single {
        get<PhoneticRoomDatabase>().providerPhoneticDao()
    }

    single {
        get<PhoneticRoomDatabase>().providerKeyTranslateDao()
    }

    single {
        Room.databaseBuilder(get(), IpaRoomDatabase::class.java, "ipa_database")
            .build().providerIpaDao()
    }

//    single {
//        Room.databaseBuilder(get(), IpaRoomDatabaseNew::class.java, "ipa_database_new")
//            .build().providerIpaDao()
//    }

    single {
        Room.databaseBuilder(get(), WordRoomDatabase::class.java, "word_database")
            .build().providerWordDao()
    }

    single {
        Room.databaseBuilder(get(), TranslateRoomDatabase::class.java, "translate_database")
            .build().providerTranslateDao()
    }
}