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
import kotlinx.coroutines.flow.first
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

        lifecycleScope.launch(handler + Dispatchers.IO) {

            val moduleName = "test2"

            val isInstalled = StartApp.isInstalled(moduleName).first()

            StartApp.downloadModuleAsync(moduleName).launchCollect(coroutineScope = lifecycleScope, context = handler + Dispatchers.IO) { state ->

                state.doSuccess {
                    logAnalytics("dynamic_feature1_download_module_${it}")
                }

                state.doFailed {
                    logCrashlytics("dynamic_feature1_download_module", it)
                }


                val status = state.toSuccess()?.data

                if (status == SplitInstallSessionStatus.INSTALLED) {

                    deleteAllModule()
                }
            }

            AutoBind.loadNameAsync(ModuleInitializer::class.java, true).launchCollect(coroutineScope = lifecycleScope, context = handler + Dispatchers.IO) {

                logAnalytics("dynamic_feature1_module_init_${isInstalled}_${it.size}")
            }

            runCatching {

                val googleApiAvailability = GoogleApiAvailability.getInstance()
                val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(PhoneticsApp.share)
                ConnectionResult.SUCCESS

                logAnalytics("dynamic_feature1_google_play_$resultCode")
            }.getOrElse {

                logCrashlytics("dynamic_feature1_google_play", it)
            }
        }
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

    private suspend fun deleteAllModule() {

        val state = StartApp.deleteAll().first()

        state.doSuccess {
            logAnalytics("dynamic_feature1_delete_status_${it}")
        }

        state.doFailed {
            logCrashlytics("dynamic_feature1_delete", it)
        }
    }
}