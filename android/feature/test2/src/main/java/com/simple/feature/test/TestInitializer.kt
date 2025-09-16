package com.simple.feature.test

import android.app.Application
import com.simple.analytics.logAnalytics
import com.simple.autobind.annotation.AutoBind
import com.simple.startapp.ModuleInitializer

@AutoBind(ModuleInitializer::class)
class TestInitializer : ModuleInitializer {

    override fun create(application: Application) {
        logAnalytics("dynamic_feature_test_initializer_init")
    }
}