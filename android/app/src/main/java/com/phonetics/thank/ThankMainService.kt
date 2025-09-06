package com.phonetics.thank

import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.hoanganhtuan95ptit.autobind.annotation.AutoBind
import com.simple.coreapp.utils.ext.handler
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.main_services.MainService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@AutoBind(MainService::class)
class ThankMainService : MainService {

    override fun setup(activity: MainActivity) {

        activity.lifecycleScope.launch(handler + Dispatchers.IO) {

            delay(5 * 1000)

            activity.viewModels<ThankViewModel>().value
        }
    }
}