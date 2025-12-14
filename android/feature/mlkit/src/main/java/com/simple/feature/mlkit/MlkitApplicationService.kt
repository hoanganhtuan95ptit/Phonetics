package com.simple.feature.mlkit

import android.app.Application
import com.google.mlkit.common.internal.CommonComponentRegistrar
import com.google.mlkit.common.sdkinternal.MlKitContext
import com.google.mlkit.nl.languageid.bundled.internal.ThickLanguageIdRegistrar
import com.google.mlkit.nl.languageid.internal.LanguageIdRegistrar
import com.google.mlkit.nl.translate.NaturalLanguageTranslateRegistrar
import com.google.mlkit.vision.common.internal.VisionCommonRegistrar
import com.google.mlkit.vision.text.internal.TextRegistrar
import com.simple.analytics.logAnalytics
import com.simple.autobind.annotation.AutoBind
import com.simple.service.ApplicationService

@AutoBind(ApplicationService::class)
class MlkitApplicationService : ApplicationService {

    override fun setup(application: Application) {

        logAnalytics("feature_mlkit_initialized")

        val registrars = listOf(
            TextRegistrar(),
            LanguageIdRegistrar(),
            VisionCommonRegistrar(),
            CommonComponentRegistrar(),
            ThickLanguageIdRegistrar(),
            NaturalLanguageTranslateRegistrar()
        )

        MlKitContext.initialize(application, registrars)
    }
}