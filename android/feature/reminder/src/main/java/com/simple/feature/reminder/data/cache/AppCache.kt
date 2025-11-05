package com.simple.feature.reminder.data.cache

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.simple.phonetics.PhoneticsApp

internal object AppCache {

    private val sharedPreferences: SharedPreferences by lazy {
        PhoneticsApp.share.getSharedPreferences("reminder", Context.MODE_PRIVATE)
    }

    fun getTimeUserInteractInHome() = sharedPreferences.getLong("TimeUserInteractInHome", System.currentTimeMillis())

    fun updateTimeUserInteractInHome() = sharedPreferences.edit { putLong("TimeUserInteractInHome", System.currentTimeMillis()) }
}