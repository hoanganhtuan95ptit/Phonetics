package com.simple.phonetics.ui

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.core.os.bundleOf
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
}