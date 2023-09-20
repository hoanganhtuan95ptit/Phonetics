package com.simple.phonetics.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.simple.analytics.logAnalytics
import com.simple.bottomsheet.ActivityScreen
import com.simple.coreapp.ui.base.activities.BaseViewBindingActivity
import com.simple.coreapp.utils.extentions.doOnHeightNavigationChange
import com.simple.coreapp.utils.extentions.getColorFromAttr
import com.simple.coreapp.utils.extentions.toPx
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.databinding.ActivityMainBinding
import com.simple.phonetics.ui.phonetics.PhoneticsFragment


class MainActivity : BaseViewBindingActivity<ActivityMainBinding>(), ActivityScreen {

    override fun onCreate(savedInstanceState: Bundle?) {

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        super.onCreate(savedInstanceState)


        lifecycleScope.launchWhenResumed {

            supportFragmentManager.beginTransaction().add(com.simple.phonetics.R.id.fragment_container, PhoneticsFragment()).commitAllowingStateLoss()
        }


        val binding = binding ?: return

        MobileAds.initialize(this) { status ->
            logAnalytics("MobileAds" to "MobileAds", *status.adapterStatusMap.map { Pair(it.key, it.value.initializationState.name) }.toTypedArray(), *status.adapterStatusMap.map { Pair(it.key, it.value.description) }.toTypedArray())
        }

        binding.adView.loadAd(AdRequest.Builder().build())
        binding.adView.adListener = object : AdListener() {

            override fun onAdClicked() {
                logAnalytics("AdClicked" to "AdClicked")
            }

            override fun onAdOpened() {
                logAnalytics("AdOpened" to "AdOpened")
            }

            override fun onAdImpression() {
                logAnalytics("AdImpression" to "AdImpression")
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                logCrashlytics(RuntimeException("${p0.code}-${p0.message}"))
            }
        }

        doOnHeightNavigationChange {

            binding.root.updatePadding(bottom = it)
        }
    }

    override fun onPercent(percent: Float) {

        val binding = binding ?: return

        binding.fragmentContainer.setRadius(percent * 40.toPx())
    }
}