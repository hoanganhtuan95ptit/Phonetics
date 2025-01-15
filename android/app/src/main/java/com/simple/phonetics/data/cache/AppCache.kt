package com.simple.phonetics.data.cache

import kotlinx.coroutines.flow.Flow

interface AppCache {

    fun setData(key: String, value: String)

    fun getDataAsync(key: String): Flow<String>


    fun <T> setData(key: String, value: T)

    fun <T> getData(key: String, default: T): T

    fun <T> getDataAsync(key: String, default: T): Flow<T>


    fun getVersionCachePhonetics(): Long

    fun saveVersionCachePhonetics(version: Long)
}