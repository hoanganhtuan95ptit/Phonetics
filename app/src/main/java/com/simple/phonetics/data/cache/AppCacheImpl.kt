package com.simple.phonetics.data.cache

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

class AppCacheImpl(
    private val context: Context,
) : AppCache {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("AppCache", Context.MODE_PRIVATE)
    }

    override fun setData(key: String, value: String) {

        sharedPreferences.edit().putString(key, value).apply()
    }

    override fun getDataAsync(key: String): Flow<String> = channelFlow {

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, _key ->

            if (key.equals(_key, true)) trySend(sharedPreferences.getString(key, "").orEmpty())
        }


        listener.onSharedPreferenceChanged(sharedPreferences, key)


        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        awaitClose {

            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    override fun <T> setData(key: String, value: T) {

        when (value) {
            is Int -> {
                sharedPreferences.edit().putInt(key, value).apply()
            }

            is Float -> {
                sharedPreferences.edit().putFloat(key, value).apply()
            }

            is Long -> {
                sharedPreferences.edit().putLong(key, value).apply()
            }

            is Boolean -> {
                sharedPreferences.edit().putBoolean(key, value).apply()
            }

            is String -> {
                sharedPreferences.edit().putString(key, value).apply()
            }
        }
    }

    override fun <T> getData(key: String, default: T): T {

        val data = when (default) {

            is Int -> {
                sharedPreferences.getInt(key, default)
            }

            is Float -> {
                sharedPreferences.getFloat(key, default)
            }

            is Long -> {
                sharedPreferences.getLong(key, default)
            }

            is Boolean -> {
                sharedPreferences.getBoolean(key, default)
            }

            is String -> {
                sharedPreferences.getString(key, default)
            }

            else -> {

                "not support"
            }
        }

        return data as T
    }

    override fun <T> getDataAsync(key: String, default: T): Flow<T> = channelFlow {

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, _key ->

            if (key.equals(_key, true)) trySend(getData(key, default))
        }


        listener.onSharedPreferenceChanged(sharedPreferences, key)


        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        awaitClose {

            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    override fun getVersionCachePhonetics(): Long {

        return getData("VERSION_CACHE_PHONETICS", -1L)
    }

    override fun saveVersionCachePhonetics(version: Long) {

        setData("VERSION_CACHE_PHONETICS", version)
    }
}