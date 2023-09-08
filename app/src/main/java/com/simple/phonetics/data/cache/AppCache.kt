package com.simple.phonetics.data.cache

interface AppCache {

    fun getVersionCachePhonetics(): Long

    fun saveVersionCachePhonetics(version: Long)
}