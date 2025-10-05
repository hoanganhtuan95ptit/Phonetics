package com.simple.phonetics.ui

import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.simple.analytics.logAnalytics
import com.simple.autobind.AutoBind
import com.simple.autobind.utils.doFailed
import com.simple.autobind.utils.doSuccess
import com.simple.autobind.utils.isCompleted
import com.simple.autobind.utils.toSuccess
import com.simple.coreapp.ui.base.activities.BaseViewModelActivity
import com.simple.coreapp.ui.base.fragments.transition.TransitionGlobalViewModel
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.crashlytics.logCrashlytics
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.PhoneticsApp
import com.simple.phonetics.databinding.ActivityMainBinding
import com.simple.phonetics.ui.view.MainView
import com.simple.startapp.ModuleInitializer
import com.simple.startapp.StartApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

                activityViewModel.awaitTransition()

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