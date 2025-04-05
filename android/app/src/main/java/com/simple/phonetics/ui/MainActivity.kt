package com.simple.phonetics.ui

import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.simple.coreapp.ui.base.activities.BaseViewModelActivity
import com.simple.coreapp.ui.base.fragments.transition.TransitionGlobalViewModel
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.phonetics.Deeplink
import com.simple.phonetics.Param
import com.simple.phonetics.databinding.ActivityMainBinding
import com.simple.phonetics.ui.view.ReadView
import com.simple.phonetics.ui.view.ReadViewImpl
import com.simple.phonetics.ui.view.SpeakView
import com.simple.phonetics.ui.view.SpeakViewImpl
import com.simple.phonetics.utils.DeeplinkView
import com.simple.phonetics.utils.DeeplinkViewImpl
import com.simple.phonetics.utils.appPhoneticCodeSelected
import com.simple.phonetics.utils.sendDeeplink
import com.simple.phonetics.utils.setupSize
import com.simple.phonetics.utils.setupTheme
import kotlinx.coroutines.launch

class MainActivity : BaseViewModelActivity<ActivityMainBinding, MainViewModel>(),
    ReadView by ReadViewImpl(),
    SpeakView by SpeakViewImpl(),
    DeeplinkView by DeeplinkViewImpl() {

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
        setupDeeplink(this)

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
    }

    private fun observeData() = with(viewModel) {

        languageInputLanguage.observe(this@MainActivity) {

            if (it == null) {

                sendDeeplink(Deeplink.LANGUAGE, extras = bundleOf(Param.FIRST to true))
            } else {

                sendDeeplink(Deeplink.PHONETICS)
            }
        }
    }

    private fun observeConfigData() = with(configViewModel) {

        phoneticSelect.observe(this@MainActivity) {

            appPhoneticCodeSelected.tryEmit(it)
        }
    }
}