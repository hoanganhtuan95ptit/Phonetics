package com.unknown.mlkit

import android.content.Context
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import com.google.mlkit.common.MlKit
import com.simple.detect.mlkit.MlkitDetectInitializer
import com.simple.translate.mlkit.MlkitTranslateInitializer

class MlkitInitializer : Initializer<Unit> {

    override fun create(context: Context) {

        MlKit.initialize(context)

        AppInitializer.getInstance(context)
            .initializeComponent(MlkitDetectInitializer::class.java)

        AppInitializer.getInstance(context)
            .initializeComponent(MlkitTranslateInitializer::class.java)

        return
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
