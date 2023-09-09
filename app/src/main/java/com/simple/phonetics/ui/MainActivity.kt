package com.simple.phonetics.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.simple.bottomsheet.ActivityScreen
import com.simple.coreapp.ui.base.activities.BaseViewBindingActivity
import com.simple.coreapp.utils.extentions.getColorFromAttr
import com.simple.coreapp.utils.extentions.toPx
import com.simple.phonetics.R
import com.simple.phonetics.databinding.ActivityMainBinding
import com.simple.phonetics.ui.phonetics.PhoneticsFragment

class MainActivity : BaseViewBindingActivity<ActivityMainBinding>(), ActivityScreen {

    override fun onCreate(savedInstanceState: Bundle?) {

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT


        super.onCreate(savedInstanceState)


        lifecycleScope.launchWhenResumed {

            supportFragmentManager.beginTransaction().add(R.id.fragment_container, PhoneticsFragment()).commitAllowingStateLoss()
        }


        val binding = binding ?: return

        (binding.root.parent as? View)?.setBackgroundColor(binding.root.context.getColorFromAttr(com.google.android.material.R.attr.colorOnBackground))
    }

    override fun onPercent(percent: Float) {

        val binding = binding ?: return

        binding.root.setRadius(percent * 40.toPx())
    }
}