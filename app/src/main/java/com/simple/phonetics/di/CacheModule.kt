package com.simple.phonetics.di

import com.simple.phonetics.data.cache.AppCache
import com.simple.phonetics.data.cache.AppCacheImpl
import org.koin.dsl.module

@JvmField
val cacheModule = module {

    single<AppCache> {
        AppCacheImpl()
    }

}