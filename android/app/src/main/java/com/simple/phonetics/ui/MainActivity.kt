package com.simple.phonetics.ui

import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.simple.coreapp.ui.base.activities.BaseViewModelActivity
import com.simple.coreapp.ui.base.fragments.transition.TransitionGlobalViewModel
import com.simple.phonetics.Deeplink
import com.simple.phonetics.databinding.ActivityMainBinding
import com.simple.phonetics.ui.view.ListenView
import com.simple.phonetics.ui.view.ListenViewImpl
import com.simple.phonetics.ui.view.SpeakView
import com.simple.phonetics.ui.view.SpeakViewImpl
import com.simple.phonetics.utils.NavigationView
import com.simple.phonetics.utils.NavigationViewImpl
import com.simple.phonetics.utils.sendDeeplink
import com.simple.phonetics.utils.setupSize
import com.simple.phonetics.utils.setupTheme
import kotlinx.coroutines.launch

class MainActivity : BaseViewModelActivity<ActivityMainBinding, MainViewModel>(),
    SpeakView by SpeakViewImpl(),
    ListenView by ListenViewImpl(),
    NavigationView by NavigationViewImpl() {

    private val activityViewModel: TransitionGlobalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupSize(this)
        setupTheme(this)
        setupSpeak(this)
        setupListen(this)
        setupNavigation(this)

        observeData()

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

                sendDeeplink(Deeplink.LANGUAGE)
            } else {

                sendDeeplink(Deeplink.PHONETICS)
            }
        }
    }
}