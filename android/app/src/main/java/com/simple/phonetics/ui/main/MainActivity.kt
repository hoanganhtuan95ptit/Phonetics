package com.simple.phonetics.ui.main

import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.lifecycle.lifecycleScope
import com.simple.analytics.logAnalytics
import com.simple.coreapp.ui.base.activities.BaseViewModelActivity
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.PhoneticsApp
import com.simple.phonetics.databinding.ActivityMainBinding
import com.simple.phonetics.ui.main.MainView
import com.simple.phonetics.ui.MainViewModel
import com.simple.phonetics.ui.base.services.transition.onTransitionRunningEndAwait
import com.unknown.coroutines.handler
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : BaseViewModelActivity<ActivityMainBinding, MainViewModel>(), MainView {

    override fun onCreate(savedInstanceState: Bundle?) {

        observeData()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) splashScreen.setOnExitAnimationListener { splashScreenView ->

            lifecycleScope.launch {

                onTransitionRunningEndAwait()

                val slideUp = ObjectAnimator.ofFloat(splashScreenView, View.ALPHA, 1f, 0f)
                slideUp.interpolator = AnticipateInterpolator()
                slideUp.duration = 350L

                slideUp.start()

                val timeInit = (System.currentTimeMillis() - PhoneticsApp.Companion.start) / 1000
                if (timeInit >= 1) logAnalytics("init_slow_$timeInit")
            }
        }

        logAnalytics("ads_init")

        viewModel.initCompleted()
    }

    private fun observeData() = with(viewModel) {

        inputLanguageFlow.launchCollect(this@MainActivity, start = CoroutineStart.UNDISPATCHED, context = handler + Dispatchers.IO) {

            initCompleted.first()

            if (it == null) {

                sendDeeplink(DeeplinkManager.LANGUAGE, extras = mapOf(Param.FIRST to true))
            } else {

                sendDeeplink(DeeplinkManager.PHONETICS)
            }
        }
    }
}