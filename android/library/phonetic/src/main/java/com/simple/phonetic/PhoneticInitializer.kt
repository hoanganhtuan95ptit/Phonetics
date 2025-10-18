package com.simple.phonetic

import android.app.Application
import android.content.Context
import androidx.startup.Initializer

class PhoneticInitializer : Initializer<Unit> {

    companion object {

       internal lateinit var application: Application
    }

    override fun create(context: Context) {

        if (context is Application) application = context

        return
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
