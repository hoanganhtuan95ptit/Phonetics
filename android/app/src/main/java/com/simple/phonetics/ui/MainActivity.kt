package com.simple.phonetics.ui

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AnticipateInterpolator
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.simple.coreapp.ui.base.activities.BaseViewModelActivity
import com.simple.phonetics.Deeplink
import com.simple.phonetics.Param
import com.simple.phonetics.databinding.ActivityMainBinding
import com.simple.phonetics.ui.view.SpeakView
import com.simple.phonetics.ui.view.SpeakViewImpl
import com.simple.phonetics.utils.NavigationView
import com.simple.phonetics.utils.NavigationViewImpl
import com.simple.phonetics.utils.sendDeeplink
import com.simple.phonetics.utils.setupTheme
import kotlinx.coroutines.launch

class MainActivity : BaseViewModelActivity<ActivityMainBinding, MainViewModel>(),
    SpeakView by SpeakViewImpl(),
    NavigationView by NavigationViewImpl() {

    private val activityViewModel: com.simple.coreapp.ui.base.fragments.transition.TransitionGlobalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        super.onCreate(savedInstanceState)

        setupTheme(this)
        setupSpeak(this)
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

                sendDeeplink(Deeplink.LANGUAGE, extras = bundleOf(Param.FIRST to true, Param.TRANSITION_DURATION to 0L))
            } else {

                sendDeeplink(Deeplink.PHONETICS, extras = bundleOf(Param.TRANSITION_DURATION to 0L))
            }
        }
    }
}