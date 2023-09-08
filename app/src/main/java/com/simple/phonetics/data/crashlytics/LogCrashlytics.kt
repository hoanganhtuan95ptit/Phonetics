package com.simple.phonetics.data.crashlytics

import android.util.Log
import com.simple.crashlytics.Crashlytics

class LogCrashlytics : Crashlytics {

    override suspend fun execute(throwable: Throwable) {

       Log.d("tuanha", "CRASHLYTICS", throwable)
    }
}