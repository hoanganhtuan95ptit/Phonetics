package com.simple.phonetics.ui

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.simple.coreapp.utils.extentions.toPx
import com.simple.phonetics.R
import com.simple.phonetics.ui.phonetics.PhoneticsFragment
import com.simple.bottomsheet.ActivityScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), ActivityScreen {


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        setContentView(R.layout.activity_main)

        lifecycleScope.launchWhenResumed {

            supportFragmentManager.beginTransaction().add(R.id.fragment_container, PhoneticsFragment()).commitAllowingStateLoss()
        }

        with(ProcessLifecycleOwner.get()) {

            lifecycleScope.launch(Dispatchers.IO) {

//                syncUseCase.execute().collect {
//
//                }
            }
        }

    }

    override fun onPercent(percent: Float) {

        findViewById<CardView>(R.id.fragment_container).setRadius(percent * 40.toPx())
    }
}