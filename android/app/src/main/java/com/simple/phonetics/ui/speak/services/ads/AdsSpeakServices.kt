package com.simple.phonetics.ui.speak.services.ads

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.simple.analytics.logAnalytics
import com.simple.autobind.annotation.AutoBind
import com.simple.coreapp.utils.ext.DP
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.R
import com.simple.phonetics.databinding.LayoutBannerAdBinding
import com.simple.phonetics.ui.main.services.ads.AdsViewModel
import com.simple.phonetics.ui.speak.SpeakFragment
import com.simple.phonetics.ui.speak.SpeakViewModel
import com.simple.phonetics.utils.exts.listenerHeightChangeAsync
import com.simple.phonetics.utils.exts.value
import com.simple.service.FragmentViewCreatedService
import com.unknown.coroutines.launchCollect
import com.unknown.size.uitls.exts.navigationBarHeight
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.androidx.viewmodel.ext.android.activityViewModel

@AutoBind(SpeakFragment::class)
class AdsSpeakServices : FragmentViewCreatedService {

    override fun setup(fragment: Fragment) {

        val speakFragment = fragment as SpeakFragment
        val viewModel = speakFragment.viewModels<SpeakViewModel>().value
        val adsViewModel = speakFragment.activityViewModel<AdsViewModel>().value

        val bindingAds = LayoutBannerAdBinding.inflate(LayoutInflater.from(speakFragment.requireContext()))

        val layoutParam = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        speakFragment.dialog?.findViewById<ViewGroup>(com.google.android.material.R.id.container)?.addView(bindingAds.root, layoutParam) ?: return


        viewModel.sizeFlow.map { it.navigationBarHeight }.distinctUntilChanged().launchCollect(fragment.viewLifecycleOwner) {

            val bindingAds = bindingAds

            bindingAds.root.updatePadding(left = DP.DP_24, top = DP.DP_16, bottom = it + DP.DP_24, right = DP.DP_24)
        }

        bindingAds.root.listenerHeightChangeAsync().launchCollect(fragment.viewLifecycleOwner) {

            val bindingAction = fragment.bindingAction ?: return@launchCollect

            bindingAction.root.updatePadding(bottom = it + DP.DP_16)
        }


        if (adsViewModel.isAdsNativeSpeakEnable.value == false) {
            logAnalytics("ads_native_speak_disable")
            return
        }


        val adLoader = AdLoader.Builder(fragment.requireContext(), fragment.getString(R.string.ad_native_id)).forNativeAd { nativeAd: NativeAd ->

            if (fragment.binding == null) return@forNativeAd

            binding(fragment = fragment, bindingAds = bindingAds, nativeAd = nativeAd)
        }.withAdListener(object : com.google.android.gms.ads.AdListener() {

            override fun onAdFailedToLoad(adError: LoadAdError) {
                // Xử lý UI: Ẩn container quảng cáo để tránh khoảng trắng
                logCrashlytics("ads_native_speak_FailedToLoad", RuntimeException(adError.toString()))
            }

            override fun onAdOpened() {
                // Tracking: Người dùng nhấn vào quảng cáo
                logAnalytics("ads_native_speak_open")
            }

            override fun onAdImpression() {
                // Tracking: Quảng cáo thực sự đã hiển thị trên màn hình người dùng
                logAnalytics("ads_native_speak_impression")
            }
        }).build()

        adLoader.loadAd(com.google.android.gms.ads.admanager.AdManagerAdRequest.Builder().build())
    }

    private fun binding(fragment: SpeakFragment, nativeAd: NativeAd, bindingAds: LayoutBannerAdBinding) = runCatching {

        val adView = fragment.getLayoutInflater().inflate(R.layout.native_banner_ad, null) as NativeAdView

        populateNativeAdView(nativeAd, adView)

        val layoutParam = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        bindingAds.root.removeAllViews()
        bindingAds.root.addView(adView, layoutParam)
    }.getOrElse {

        logCrashlytics("binding", it)
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) = runCatching {

        // Đăng ký các view với NativeAdView
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)

        // Đổ dữ liệu từ nativeAd vào view
        (adView.headlineView as TextView).text = nativeAd.headline
        (adView.bodyView as TextView).text = nativeAd.body
        (adView.callToActionView as Button).text = nativeAd.callToAction

        if (nativeAd.icon != null) {
            (adView.iconView as ImageView).setImageDrawable(nativeAd.icon!!.drawable)
        }

        // Quan trọng: Gán đối tượng nativeAd cho view để hoàn tất
        adView.setNativeAd(nativeAd)
    }.getOrElse {

        logCrashlytics("populateNativeAdView", it)
    }
}