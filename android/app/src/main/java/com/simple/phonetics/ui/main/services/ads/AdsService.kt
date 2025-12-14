package com.simple.phonetics.ui.main.services.ads

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.simple.analytics.logAnalytics
import com.simple.autobind.annotation.AutoBind
import com.simple.config.phonetics.R
import com.simple.coreapp.utils.ext.handler
import com.simple.phonetics.DeeplinkManager.ADS
import com.simple.phonetics.ui.main.MainActivity
import com.simple.phonetics.ui.main.services.MainService
import com.simple.phonetics.utils.sendDeeplinkWithThank
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel


@AutoBind(MainActivity::class)
class AdsService : MainService {

    override fun setup(mainActivity: MainActivity) {

        val adsViewModel by mainActivity.viewModel<AdsViewModel>()


        val adsInit = MediatorLiveData<Unit>()


        mainActivity.lifecycleScope.launch(handler + Dispatchers.IO) {

            MobileAds.initialize(mainActivity) {

                adsInit.postValue(Unit)
            }
        }

        adsViewModel.show.asFlow().launchCollect(mainActivity) {


            adsInit.asFlow().first()


            it.getContentIfNotHandled() ?: return@launchCollect

            runCatching {

                showInterstitialAd(mainActivity).first()
                adsViewModel.countShow()
            }
        }
    }

    private fun showInterstitialAd(activity: MainActivity) = channelFlow {

        logAnalytics("ads_onAdStart")

        val interstitialAd = channelFlow {

            InterstitialAd.load(activity, activity.getString(R.string.ad_unit_id), AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {

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