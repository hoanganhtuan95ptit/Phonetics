package com.simple.phonetics.ui

import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.simple.analytics.logAnalytics
import com.simple.coreapp.ui.base.activities.BaseViewModelActivity
import com.simple.coreapp.ui.base.fragments.transition.TransitionGlobalViewModel
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.PhoneticsApp
import com.simple.phonetics.databinding.ActivityMainBinding
import com.simple.phonetics.ui.base.services.transition.onTransitionEndAwait
import com.simple.phonetics.ui.view.MainView
import kotlinx.coroutines.launch
import java.util.ServiceLoader

class MainActivity : BaseViewModelActivity<ActivityMainBinding, MainViewModel>() {


    private val activityViewModel: TransitionGlobalViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ServiceLoader.load(MainView::class.java).toList().forEach { it.setup(this) }

        observeData()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) splashScreen.setOnExitAnimationListener { splashScreenView ->

            lifecycleScope.launch {

                onTransitionEndAwait()

                val slideUp = ObjectAnimator.ofFloat(splashScreenView, View.ALPHA, 1f, 0f)
                slideUp.interpolator = AnticipateInterpolator()
                slideUp.duration = 350L

                slideUp.start()

                val timeInit = (System.currentTimeMillis() - PhoneticsApp.start) / 1000
                if (timeInit >= 1) logAnalytics("init_slow_$timeInit")
            }
        }

        logAnalytics("ads_init")
    }

    private fun observeData() = with(viewModel) {

        languageInputLanguage.observe(this@MainActivity) {

            if (it == null) {

                sendDeeplink(DeeplinkManager.LANGUAGE, extras = mapOf(Param.FIRST to true))
            } else {

                sendDeeplink(DeeplinkManager.PHONETICS)
            }
        }
    }
}