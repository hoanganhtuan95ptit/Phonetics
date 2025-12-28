package com.simple.phonetics.ui.main.services.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.simple.autobind.annotation.AutoBind
import com.simple.core.utils.extentions.toJson
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.PhoneticsApp
import com.simple.phonetics.ui.base.services.transition.locking.exts.getTransitionLockInfo
import com.simple.phonetics.ui.base.services.transition.locking.exts.onTransitionLockEndAwait
import com.simple.phonetics.ui.base.services.transition.running.exts.getTransitionRunningInfo
import com.simple.phonetics.ui.main.MainActivity
import com.simple.phonetics.ui.main.services.MainService
import com.simple.phonetics.utils.exts.startWithTransition
import com.unknown.coroutines.handler
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


@AutoBind(MainActivity::class)
class SplashService : MainService {

    override fun priority(): Int {
        return Int.MIN_VALUE
    }

    override fun setup(mainActivity: MainActivity) = mainActivity.splashScreen.observe(mainActivity) { view ->

        val splashScreen = view ?: return@observe

        val transitionTrackingJob = mainActivity.lifecycleScope.launch(handler + Dispatchers.IO) {

            var count = 0
            while (isActive) {
                delay(500)

                if (count > 1) trackingTransition(mainActivity = mainActivity, count = count)
                count++
            }
        }

        splashScreen.onExitAnimationStream().onEach {

            transitionTrackingJob.cancel()
        }.filterNotNull().launchCollect(mainActivity) { splashScreenView ->

            trackingReady(mainActivity = mainActivity)

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
            animatorSet.startWithTransition(mainActivity) {
                splashScreenView.remove()
            }
        }


        val isReady = MutableLiveData(false)

        mainActivity.lifecycleScope.launch {

            mainActivity.onTransitionLockEndAwait()

            isReady.postValue(true)
        }

        splashScreen.setKeepOnScreenCondition {

            !(isReady.value ?: false)
        }
    }

    private fun trackingReady(mainActivity: MainActivity) {

        val timeInit = System.currentTimeMillis() - PhoneticsApp.Companion.start

        val transitionLock by lazy {
            mainActivity.getTransitionLockInfo().takeIf { it.isNotEmpty() }?.toJson()?.let { "lock:$it" }
        }
        val transitionRunning by lazy {
            mainActivity.getTransitionRunningInfo().takeIf { it.isNotEmpty() }?.toJson()?.let { "running:$it" }
        }

        if (timeInit > 6000) {
            logCrashlytics("init_slow_6", RuntimeException("timeInit:$timeInit ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        } else if (timeInit > 5000) {
            logCrashlytics("init_slow_5", RuntimeException("timeInit:$timeInit ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        } else if (timeInit > 4000) {
            logCrashlytics("init_slow_4", RuntimeException("timeInit:$timeInit ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        } else if (timeInit > 3000) {
            logCrashlytics("init_slow_3", RuntimeException("timeInit:$timeInit ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        } else if (timeInit >= 2000) {
            logCrashlytics("init_slow_2", RuntimeException("timeInit:$timeInit ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        }
    }

    private fun trackingTransition(mainActivity: MainActivity, count: Int) {

        val transitionLock = mainActivity.getTransitionLockInfo().takeIf { it.isNotEmpty() }?.toJson()?.let { "lock:$it" }
        val transitionRunning = mainActivity.getTransitionRunningInfo().takeIf { it.isNotEmpty() }?.toJson()?.let { "running:$it" }

        if (transitionLock != null || transitionRunning != null) if (count > 20) {
            logCrashlytics("transition_tracking_20", RuntimeException("count:$count ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        } else if (count > 18) {
            logCrashlytics("transition_tracking_18", RuntimeException("count:$count ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        } else if (count > 16) {
            logCrashlytics("transition_tracking_16", RuntimeException("count:$count ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        } else if (count > 14) {
            logCrashlytics("transition_tracking_14", RuntimeException("count:$count ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        } else if (count > 12) {
            logCrashlytics("transition_tracking_12", RuntimeException("count:$count ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        } else if (count > 10) {
            logCrashlytics("transition_tracking_10", RuntimeException("count:$count ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        } else if (count > 8) {
            logCrashlytics("transition_tracking_8", RuntimeException("count:$count ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        } else if (count > 6) {
            logCrashlytics("transition_tracking_6", RuntimeException("count:$count ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        } else if (count > 4) {
            logCrashlytics("transition_tracking_4", RuntimeException("count:$count ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        } else if (count > 2) {
            logCrashlytics("transition_tracking_2", RuntimeException("count:$count ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        }
    }

    private fun SplashScreen.onExitAnimationStream(): Flow<SplashScreenViewProvider?> = channelFlow {

        setOnExitAnimationListener { splashScreenView ->

            trySend(splashScreenView)
        }

        awaitClose {

        }
    }
}