package com.simple.phonetics.ui.main_services

import android.animation.ObjectAnimator
import android.os.Build
import android.view.View
import android.view.animation.AnticipateInterpolator
import android.window.SplashScreenView
import androidx.lifecycle.lifecycleScope
import com.hoanganhtuan95ptit.autobind.annotation.AutoBind
import com.simple.analytics.logAnalytics
import com.simple.phonetics.PhoneticsApp
import com.simple.phonetics.ui.MainActivity
import kotlinx.coroutines.launch

@AutoBind(MainService::class)
class SplashService : MainService {

    override fun setup(activity: MainActivity) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) activity.splashScreen.setOnExitAnimationListener { splashScreenView ->

            exitSplash(activity, splashScreenView)
        }
    }

    private fun exitSplash(activity: MainActivity, splashScreenView: SplashScreenView) = activity.lifecycleScope.launch {

        activity.activityViewModel.awaitTransition()

        val slideUp = ObjectAnimator.ofFloat(splashScreenView, View.ALPHA, 1f, 0f)
        slideUp.interpolator = AnticipateInterpolator()
        slideUp.duration = 350L

        slideUp.start()

        val timeInit = (System.currentTimeMillis() - PhoneticsApp.start) / 1000
        if (timeInit >= 1) logAnalytics("init_slow_$timeInit")
    }
}