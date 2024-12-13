package com.simple.phonetics.ui

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AnticipateInterpolator
import androidx.core.animation.doOnEnd
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
import com.simple.phonetics.utils.changeTheme
import com.simple.phonetics.utils.sendDeeplink
import com.simple.phonetics.utils.setupTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : BaseViewModelActivity<ActivityMainBinding, MainViewModel>(),
    SpeakView by SpeakViewImpl(),
    NavigationView by NavigationViewImpl() {

    override fun onCreate(savedInstanceState: Bundle?) {

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        super.onCreate(savedInstanceState)

        setupTheme(this)
        setupSpeak(this)
        setupNavigation(this)

        observeData()

//        lifecycleScope.launch {
//
//            delay(5 * 1000)
//            changeTheme(activity = this@MainActivity)
//        }
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