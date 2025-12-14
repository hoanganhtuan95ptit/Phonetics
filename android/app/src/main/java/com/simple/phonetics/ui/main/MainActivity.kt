package com.simple.phonetics.ui.main

import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.lifecycle.lifecycleScope
import com.simple.analytics.logAnalytics
import com.simple.core.utils.extentions.toJson
import com.simple.coreapp.ui.base.activities.BaseViewModelActivity
import com.simple.crashlytics.logCrashlytics
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.PhoneticsApp
import com.simple.phonetics.databinding.ActivityMainBinding
import com.simple.phonetics.ui.MainViewModel
import com.simple.phonetics.ui.base.services.transition.locking.exts.getTransitionLockInfo
import com.simple.phonetics.ui.base.services.transition.running.exts.getTransitionRunningInfo
import com.simple.phonetics.ui.base.services.transition.running.exts.onTransitionRunningEndAwait
import com.unknown.coroutines.handler
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : BaseViewModelActivity<ActivityMainBinding, MainViewModel>() {

    override fun onCreate(savedInstanceState: Bundle?) {

        observeData()
        super.onCreate(savedInstanceState)

        val transitionCheckJob = lifecycleScope.launch {

            var count = 0
            while (isActive) {
                delay(500)

                val transitionLock = getTransitionLockInfo().takeIf { it.isNotEmpty() }?.toJson()?.let { "lock:$it" }
                val transitionRunning = getTransitionRunningInfo().takeIf { it.isNotEmpty() }?.toJson()?.let { "running:$it" }

                if (transitionLock == null && transitionRunning == null) continue

                val message = "count:$count ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()

                logCrashlytics("transition_tracking", RuntimeException(message))
                count++
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) splashScreen.setOnExitAnimationListener { splashScreenView ->

            lifecycleScope.launch {

                onTransitionRunningEndAwait()

                val slideUp = ObjectAnimator.ofFloat(splashScreenView, View.ALPHA, 1f, 0f)
                slideUp.interpolator = AnticipateInterpolator()
                slideUp.duration = 350L

                slideUp.start()

                transitionCheckJob.cancel()

                val timeInit = (System.currentTimeMillis() - PhoneticsApp.Companion.start) / 1000
                if (timeInit >= 1) logAnalytics("init_slow_$timeInit")
            }
        } else {

            transitionCheckJob.cancel()
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