package com.simple.phonetics.ui

import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.simple.coreapp.ui.base.activities.BaseViewModelActivity
import com.simple.coreapp.ui.base.fragments.transition.TransitionGlobalViewModel
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.ButtonInfo
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.ext.with
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.R
import com.simple.phonetics.databinding.ActivityMainBinding
import com.simple.phonetics.ui.view.ReadView
import com.simple.phonetics.ui.view.ReadViewImpl
import com.simple.phonetics.ui.view.SpeakView
import com.simple.phonetics.ui.view.SpeakViewImpl
import com.simple.phonetics.utils.appPhoneticCodeSelected
import com.simple.phonetics.utils.appTheme
import com.simple.phonetics.utils.setupSize
import com.simple.phonetics.utils.setupTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : BaseViewModelActivity<ActivityMainBinding, MainViewModel>(),
    ReadView by ReadViewImpl(),
    SpeakView by SpeakViewImpl() {

    private val configViewModel: ConfigViewModel by lazy {
        getViewModel(this, ConfigViewModel::class)
    }

    private val activityViewModel: TransitionGlobalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupSize(this)
        setupRead(this)
        setupTheme(this)
        setupSpeak(this)

        observeData()
        observeConfigData()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) splashScreen.setOnExitAnimationListener { splashScreenView ->

            lifecycleScope.launch {

                activityViewModel.awaitTransition()

                val slideUp = ObjectAnimator.ofFloat(splashScreenView, View.ALPHA, 1f, 0f)
                slideUp.interpolator = AnticipateInterpolator()
                slideUp.duration = 350L

                slideUp.start()
            }
        }

        lifecycleScope.launch {

            val theme = appTheme.firstOrNull() ?: return@launch

            val extras = mapOf(

                com.simple.coreapp.Param.CANCEL to true,

                com.simple.coreapp.Param.ANIM to R.raw.anim_rate,

                com.simple.coreapp.Param.TITLE to "Hoàng Anh Tuấn"
                    .with(ForegroundColorSpan(theme.colorOnBackground)),
                com.simple.coreapp.Param.MESSAGE to "That's it! The first time you request a project JitPack checks out the code, builds it and serves the build artifacts (jar, aar)."
                    .with(ForegroundColorSpan(theme.colorOnBackgroundVariant)),

                com.simple.coreapp.Param.POSITIVE to ButtonInfo(
                    text = "Test",
                    background = Background(
                        backgroundColor = theme.colorPrimary,
                        cornerRadius = DP.DP_24,
                    )
                ),
            )

            sendDeeplink(DeeplinkManager.CONFIRM, extras = extras)
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

    private fun observeConfigData() = with(configViewModel) {

        phoneticSelect.observe(this@MainActivity) {

            appPhoneticCodeSelected.tryEmit(it)
        }
    }
}