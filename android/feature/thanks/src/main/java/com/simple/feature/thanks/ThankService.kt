package com.simple.feature.thanks

import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.simple.analytics.logAnalytics
import com.simple.autobind.annotation.AutoBind
import com.simple.coreapp.utils.ext.handler
import com.simple.phonetics.ui.main.MainActivity
import com.simple.phonetics.ui.main.services.MainService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@AutoBind(MainActivity::class)
class ThankService : MainService {

    override fun setup(mainActivity: MainActivity) {

        logAnalytics("feature_thank_initialized")

        mainActivity.lifecycleScope.launch(handler + Dispatchers.IO) {

            delay(5 * 1000)

            mainActivity.viewModels<ThankViewModel>().value
        }
    }
}