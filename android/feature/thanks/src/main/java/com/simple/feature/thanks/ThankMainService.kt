package com.simple.feature.thanks

import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.simple.autobind.annotation.AutoBind
import com.simple.coreapp.utils.ext.handler
import com.simple.phonetics.ui.main.MainActivity
import com.simple.service.ActivityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@AutoBind(MainActivity::class)
class ThankMainService : ActivityService {

    override fun setup(fragmentActivity: FragmentActivity) {

        if (fragmentActivity !is MainActivity) {
            return
        }

        fragmentActivity.lifecycleScope.launch(handler + Dispatchers.IO) {

            delay(5 * 1000)

            fragmentActivity.viewModels<ThankViewModel>().value
        }
    }
}