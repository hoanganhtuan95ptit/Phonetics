package com.simple.phonetics.ui.main

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
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
import com.simple.phonetics.ui.base.services.transition.locking.exts.onTransitionLockEndAwait
import com.simple.phonetics.ui.base.services.transition.running.exts.getTransitionRunningInfo
import com.simple.phonetics.utils.exts.startWithTransition
import com.unknown.coroutines.handler
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : BaseViewModelActivity<ActivityMainBinding, MainViewModel>(), MainView {

    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        setupSplash(splashScreen = splashScreen)

        observeData()
    }

    private fun setupSplash(splashScreen: SplashScreen) {

        val isReady = MutableLiveData(false)


        val transitionTrackingJob = lifecycleScope.launch(handler + Dispatchers.IO) {

            var count = 0
            while (isActive) {
                delay(500)

                trackingTransition(count = count)
                count++
            }
        }

        splashScreen.onExitAnimationStream(isReady).onEach {

            transitionTrackingJob.cancel()
        }.filterNotNull().launchCollect(this) { splashScreenView ->

            // Tạo hiệu ứng mờ dần
            val alpha = ObjectAnimator.ofFloat(splashScreenView.view, View.ALPHA, 1f, 0f)

            // Tạo hiệu ứng thu nhỏ icon
            val scaleX = ObjectAnimator.ofFloat(splashScreenView.iconView, View.SCALE_X, 1f, 0f)
            val scaleY = ObjectAnimator.ofFloat(splashScreenView.iconView, View.SCALE_Y, 1f, 0f)

            // Chạy các hiệu ứng cùng lúc
            val animatorSet = AnimatorSet()
            animatorSet.duration = 350L
            animatorSet.playTogether(alpha, scaleX, scaleY)

            // QUAN TRỌNG: Phải gọi remove() khi kết thúc animation
            animatorSet.startWithTransition(this) {
                splashScreenView.remove()
            }

            trackingReady()
        }

        lifecycleScope.launch {

            onTransitionLockEndAwait()

            isReady.postValue(true)
        }

        splashScreen.setKeepOnScreenCondition {

            !(isReady.value ?: false)
        }
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

    private fun trackingReady() {

        val timeInit = System.currentTimeMillis() - PhoneticsApp.Companion.start

        val transitionLock by lazy {
            getTransitionLockInfo().takeIf { it.isNotEmpty() }?.toJson()?.let { "lock:$it" }
        }
        val transitionRunning by lazy {
            getTransitionRunningInfo().takeIf { it.isNotEmpty() }?.toJson()?.let { "running:$it" }
        }

        if (timeInit >= 1000) {
            logCrashlytics("init_slow", RuntimeException("timeInit:$timeInit ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        }
    }

    private fun trackingTransition(count: Int) {

        val transitionLock = getTransitionLockInfo().takeIf { it.isNotEmpty() }?.toJson()?.let { "lock:$it" }
        val transitionRunning = getTransitionRunningInfo().takeIf { it.isNotEmpty() }?.toJson()?.let { "running:$it" }

        if (transitionLock != null || transitionRunning != null) {
            logCrashlytics("transition_tracking", RuntimeException("count:$count ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        }
    }

    private fun SplashScreen.onExitAnimationStream(isReady: MutableLiveData<Boolean>): Flow<SplashScreenViewProvider?> = channelFlow {

        val timeoutJob = launch {

            isReady.asFlow().filter { it }.first()

            delay(600)
            trySend(null)
        }

        setOnExitAnimationListener { splashScreenView ->

            timeoutJob.cancel()
            trySend(splashScreenView)
        }

        awaitClose {

        }
    }
}