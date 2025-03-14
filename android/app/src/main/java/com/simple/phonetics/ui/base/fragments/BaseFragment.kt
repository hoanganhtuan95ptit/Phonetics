package com.simple.phonetics.ui.base.fragments

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.simple.coreapp.ui.base.fragments.transition.TransitionFragment

abstract class BaseFragment<T : androidx.viewbinding.ViewBinding, VM : BaseViewModel>(@androidx.annotation.LayoutRes contentLayoutId: Int = 0) : TransitionFragment<T, VM>(contentLayoutId) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val window = activity?.window ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) window.decorView.setOnApplyWindowInsetsListener { view, insets ->

            // Adjust padding to avoid overlap
            view.setPadding(0, 0, 0, 0)

            insets
        } else {
            // For Android 14 and below
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            WindowCompat.setDecorFitsSystemWindows(window, false)

            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {

            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }
}