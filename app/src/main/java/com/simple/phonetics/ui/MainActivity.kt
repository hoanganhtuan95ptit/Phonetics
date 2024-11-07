package com.simple.phonetics.ui

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.simple.coreapp.ui.base.activities.BaseViewModelActivity
import com.simple.phonetics.databinding.ActivityMainBinding
import com.simple.phonetics.ui.phonetics.PhoneticsFragment
import com.simple.phonetics.ui.view.SpeakView
import com.simple.phonetics.ui.view.SpeakViewImpl
import com.simple.phonetics.utils.setupTheme


class MainActivity : BaseViewModelActivity<ActivityMainBinding, MainViewModel>(),
    SpeakView by SpeakViewImpl() {

    override fun onCreate(savedInstanceState: Bundle?) {

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        super.onCreate(savedInstanceState)


        lifecycleScope.launchWhenResumed {

            supportFragmentManager.beginTransaction().add(com.simple.phonetics.R.id.fragment_container, PhoneticsFragment()).commitAllowingStateLoss()
        }

        setupSpeak(this)
        setupTheme(this)
    }
}