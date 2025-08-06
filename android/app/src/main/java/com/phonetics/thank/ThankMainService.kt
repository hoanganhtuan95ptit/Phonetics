package com.phonetics.thank

import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.auto.service.AutoService
import com.simple.coreapp.utils.ext.handler
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.view.MainView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@AutoService(MainView::class)
class ThankMainService : MainView {

    override fun setup(activity: MainActivity) {

        activity.lifecycleScope.launch(handler + Dispatchers.IO) {

            delay(5 * 1000)

            activity.viewModels<ThankViewModel>().value
        }
    }
}