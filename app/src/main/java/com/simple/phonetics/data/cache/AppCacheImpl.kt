package com.simple.phonetics.data.cache

import com.simple.coreapp.data.cache.sharedpreference.BaseCacheImpl

class AppCacheImpl : BaseCacheImpl(), AppCache {

    override fun getVersionCachePhonetics(): Long {

        return getLong("VERSION_CACHE_PHONETICS", -1) ?: -1L
    }

    override fun saveVersionCachePhonetics(version: Long) {

        putLong("VERSION_CACHE_PHONETICS", version)
    }
}