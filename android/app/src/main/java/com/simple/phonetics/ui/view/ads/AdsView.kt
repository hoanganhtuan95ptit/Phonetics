package com.simple.phonetics.ui.view.ads

import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.auto.service.AutoService
import com.simple.analytics.logAnalytics
import com.simple.core.utils.extentions.toJson
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.phonetics.DeeplinkManager.ADS
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.view.MainView
import com.simple.phonetics.utils.sendDeeplinkWithThank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel


@AutoService(MainView::class)
class AdsView : MainView {

    override fun setup(activity: MainActivity) {

        val adsViewModel by activity.viewModel<AdsViewModel>()

        activity.lifecycleScope.launch(handler + Dispatchers.IO) {

            MobileAds.initialize(activity) { initializationStatus ->

                logAnalytics("ads_onAdInitialize ${initializationStatus.adapterStatusMap.toList().sortedBy { it.first }.toMap().toJson()}")
            }
        }

        adsViewModel.show.asFlow().launchCollect(activity) {

            it.getContentIfNotHandled() ?: return@launchCollect

            runCatching {

                showInterstitialAd(activity).first()
                adsViewModel.countShow()
            }
        }
    }

    private fun showInterstitialAd(activity: MainActivity) = channelFlow {

        logAnalytics("ads_onAdStart")

        val interstitialAd = channelFlow {

            InterstitialAd.load(activity, activity.getString(com.simple.config.phonetics.R.string.ad_unit_id), AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {

                override fun onAdLoaded(interstitialAd: InterstitialAd) {

                    logAnalytics("ads_onAdLoaded")

                    trySend(interstitialAd)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {

                    logAnalytics("ads_onAdFailedToLoad ${loadAdError.code}")

                    trySend(null)
                }
            })

            awaitClose()
        }.first()


        if (interstitialAd == null) {

            logAnalytics("ads_onAdNull")
            trySend(Unit)
            awaitClose()
            return@channelFlow
        }

        interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdClicked() {

                logAnalytics("ads_onAdClicked")
            }

            override fun onAdImpression() {

                logAnalytics("ads_onAdImpression")
            }

            override fun onAdDismissedFullScreenContent() {

                logAnalytics("ads_onAdDismissedFullScreenContent")

                trySend(Unit)
                sendDeeplinkWithThank(ADS)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {

                logAnalytics("ads_onAdFailedToShowFullScreenContent ${adError.code}")

                trySend(Unit)
            }

            override fun onAdShowedFullScreenContent() {

                logAnalytics("ads_onAdShowedFullScreenContent")
            }
        }

        logAnalytics("ads_onAdShow")
        interstitialAd.show(activity)

        awaitClose {
        }
    }
}