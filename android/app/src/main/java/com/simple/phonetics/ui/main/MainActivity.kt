package com.simple.phonetics.ui.main

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.MediatorLiveData
import com.simple.coreapp.ui.base.activities.BaseViewModelActivity
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.databinding.ActivityMainBinding
import com.simple.phonetics.ui.MainViewModel
import com.unknown.coroutines.handler
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers

class MainActivity : BaseViewModelActivity<ActivityMainBinding, MainViewModel>() {

    val splashScreen = MediatorLiveData<SplashScreen?>()

    override fun onCreate(savedInstanceState: Bundle?) {

        splashScreen.value = installSplashScreen()

        super.onCreate(savedInstanceState)

        observeData()
    }

    override fun onDestroy() {
        super.onDestroy()
        splashScreen.value = null
    }

    private fun observeData() = with(viewModel) {

        inputLanguageFlow.launchCollect(this@MainActivity, start = CoroutineStart.UNDISPATCHED, context = handler + Dispatchers.IO) {

            if (it == null) {

                sendDeeplink(DeeplinkManager.LANGUAGE, extras = mapOf(Param.FIRST to true))
            } else {

                sendDeeplink(DeeplinkManager.PHONETICS)
            }
        }
    }
}