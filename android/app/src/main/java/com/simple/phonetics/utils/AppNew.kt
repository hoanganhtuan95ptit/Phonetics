package com.simple.phonetics.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.simple.phonetics.PhoneticsApp

private interface NewCache {

    fun setData(key: String, value: Long)

    fun getData(key: String): Long

    companion object {

        val instant by lazy {
            NewCacheImpl() as NewCache
        }
    }
}

private class NewCacheImpl() : NewCache {

    private val sharedPreferences: SharedPreferences by lazy {
        PhoneticsApp.share.getSharedPreferences("New", Context.MODE_PRIVATE)
    }

    override fun getData(key: String): Long {
        return sharedPreferences.getLong(key, -1)
    }

    override fun setData(key: String, value: Long) {
        sharedPreferences.edit { putLong(key, value) }
    }
}

object AppNew {

    fun isNew(key: String, timeout: Long): Boolean {

        val now = System.currentTimeMillis()

        val time = NewCache.instant.getData(key = key)


        return if (time <= 0) {

            NewCache.instant.setData(key = key, value = now)
            true
        } else {

            now - time < timeout
        }
    }
}