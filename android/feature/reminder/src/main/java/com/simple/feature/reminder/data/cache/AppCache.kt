package com.simple.feature.reminder.data.cache

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.simple.phonetics.PhoneticsApp
import java.util.Calendar

internal object AppCache {

    private val sharedPreferences: SharedPreferences by lazy {
        PhoneticsApp.share.getSharedPreferences("reminder", Context.MODE_PRIVATE)
    }

    fun getTimeUserInteractInHome() = sharedPreferences.getLong("TimeUserInteractInHome", System.currentTimeMillis())

    fun updateTimeUserInteractInHome() = sharedPreferences.edit { putLong("TimeUserInteractInHome", System.currentTimeMillis()) }


    fun getDateMorningReminder() = sharedPreferences.getInt("DateMorningReminder", 0)

    fun updateDateMorningReminder() = sharedPreferences.edit { putInt("DateMorningReminder", Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) }
}