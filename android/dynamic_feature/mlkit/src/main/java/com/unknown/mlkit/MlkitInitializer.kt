package com.unknown.mlkit

import android.content.Context
import androidx.startup.Initializer


class MlkitInitializer : Initializer<Unit> {

    override fun create(context: Context) {

//        AppInitializer.getInstance(context)
//            .initializeComponent(MlkitDetectInitializer::class.java)
//
//        AppInitializer.getInstance(context)
//            .initializeComponent(MlkitTranslateInitializer::class.java)

        return
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
