package com.simple.phonetics.ui

import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.simple.phonetics.ui.base.services.transition.onTransitionRunningEndAwait
import com.simple.phonetics.ui.view.MainView
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.launch
import java.util.ServiceLoader

class MainActivity : BaseViewModelActivity<ActivityMainBinding, MainViewModel>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("tuanha", "onCreate: LockTransitionService  0  -> ${System.currentTimeMillis() - PhoneticsApp.start}")

        observeData()
        super.onCreate(savedInstanceState)

        Log.d("tuanha", "onCreate: LockTransitionService  1  -> ${System.currentTimeMillis() - PhoneticsApp.start}")

        ServiceLoader.load(MainView::class.java).toList().forEach { it.setup(this) }

        Log.d("tuanha", "onCreate: LockTransitionService  2  -> ${System.currentTimeMillis() - PhoneticsApp.start}")


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) splashScreen.setOnExitAnimationListener { splashScreenView ->

            lifecycleScope.launch {

                onTransitionRunningEndAwait()

                val slideUp = ObjectAnimator.ofFloat(splashScreenView, View.ALPHA, 1f, 0f)
                slideUp.interpolator = AnticipateInterpolator()
                slideUp.duration = 350L

                slideUp.start()

                val timeInit = (System.currentTimeMillis() - PhoneticsApp.start) / 1000
                if (timeInit >= 1) logAnalytics("init_slow_$timeInit")
            }
        }

        logAnalytics("ads_init")

        Log.d("tuanha", "onCreate: LockTransitionService  3  -> ${System.currentTimeMillis() - PhoneticsApp.start}")
    }

    private fun observeData() = with(viewModel) {

        Log.d("tuanha", "observeData: LockTransitionService  1  -> ${System.currentTimeMillis() - PhoneticsApp.start}")

        openLanguage.launchCollect(this@MainActivity) {

            Log.d("tuanha", "observeData: LockTransitionService ${System.currentTimeMillis() - PhoneticsApp.start}")
            if (it) {

                sendDeeplink(DeeplinkManager.LANGUAGE, extras = mapOf(Param.FIRST to true))
            } else {

                sendDeeplink(DeeplinkManager.PHONETICS)
            }
        }
    }
}