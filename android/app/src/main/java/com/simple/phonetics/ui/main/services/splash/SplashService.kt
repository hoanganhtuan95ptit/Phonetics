package com.simple.phonetics.ui.main.services.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


@AutoBind(MainActivity::class)
class SplashService : MainService {

    override fun priority(): Int {
        return Int.MIN_VALUE
    }

    override fun setup(mainActivity: MainActivity) = mainActivity.splashScreen.observe(mainActivity) { view ->


        val splashScreen = view ?: return@observe


        val isReady = MutableLiveData(false)

        mainActivity.lifecycleScope.launch {

            mainActivity.onTransitionLockEndAwait()

            isReady.postValue(true)
        }

        val transitionTrackingJob = mainActivity.lifecycleScope.launch(handler + Dispatchers.IO) {

            var count = 0
            while (isActive) {
                delay(500)

                if (count > 1) trackingTransition(mainActivity = mainActivity, count = count)
                count++
            }
        }

        isReady.asFlow().filter { it }.launchCollect(mainActivity) {

            trackingReady(mainActivity = mainActivity)

            transitionTrackingJob.cancel()
        }


        splashScreen.setOnExitAnimationListener { splashScreenView ->

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

        if (timeInit >= 2000) {
            logCrashlytics("init_slow_$timeInit", RuntimeException("timeInit:$timeInit ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        }
    }

    private fun trackingTransition(mainActivity: MainActivity, count: Int) {

        val transitionLock = mainActivity.getTransitionLockInfo().takeIf { it.isNotEmpty() }?.toJson()?.let { "lock:$it" }
        val transitionRunning = mainActivity.getTransitionRunningInfo().takeIf { it.isNotEmpty() }?.toJson()?.let { "running:$it" }

        if (transitionLock != null || transitionRunning != null) if (count > 2) {
            logCrashlytics("transition_tracking_$count", RuntimeException("count:$count ${transitionLock.orEmpty()} \n ${transitionRunning.orEmpty()}".trim()))
        }
    }
}