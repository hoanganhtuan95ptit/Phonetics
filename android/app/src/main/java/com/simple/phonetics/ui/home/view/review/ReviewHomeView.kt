package com.simple.phonetics.ui.home.view.review

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.review.ReviewManagerFactory
import com.simple.analytics.logAnalytics
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.core.utils.extentions.toJson
import com.simple.core.utils.extentions.toObjectOrNull
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.crashlytics.logCrashlytics
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.PhoneticsApp
import com.simple.phonetics.ui.home.HomeFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Calendar


interface ReviewHomeView {

    fun setupReview(fragment: HomeFragment)

    fun showReview()
}

class ReviewHomeViewImpl : ReviewHomeView {

    private val show: LiveData<Boolean> = MediatorLiveData()

    override fun setupReview(fragment: HomeFragment) {

        val viewModel: ReviewHomeViewModel by fragment.viewModel()

        val keyRequest = "RATE_KEY_REQUEST"

        viewModel.rateInfoEvent.asFlow().launchCollect(fragment.viewLifecycleOwner) { event ->

            show.asFlow().firstOrNull()

            val info = event.getContentIfNotHandled() ?: return@launchCollect

            val extras = mapOf(
                com.simple.coreapp.Param.CANCEL to false,

                com.simple.coreapp.Param.POSITIVE to info.positive,
                com.simple.coreapp.Param.NEGATIVE to info.negative,

                com.simple.coreapp.Param.KEY_REQUEST to keyRequest,

                Param.VIEW_ITEM_LIST to info.viewItemList
            )

            if (info.show) {

                logAnalytics("open_rate_confirm")
            }

            if (info.show) {

                sendDeeplink(DeeplinkManager.CONFIRM + "?code:review", extras = extras)
            } else {

                openReview(fragment)
            }
        }

        val sharedPreferences: SharedPreferences by lazy {
            PhoneticsApp.share.getSharedPreferences("AppReview", Context.MODE_PRIVATE)
        }

        com.simple.event.listenerEvent(fragment.viewLifecycleOwner.lifecycle, keyRequest) {

            val resultCode = it.asObjectOrNull<Int>() ?: return@listenerEvent

            logAnalytics("rate_confirm_with_result_code_$resultCode")

            if (resultCode == 1) {

                openStore(fragment)
            }

            val rate = if (resultCode == 1) ReviewHomeViewModel.Rate(
                status = ReviewHomeViewModel.Rate.Status.OPEN_RATE.value
            ) else ReviewHomeViewModel.Rate(
                status = ReviewHomeViewModel.Rate.Status.DISMISS.value,
                date = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            )

            sharedPreferences.edit()
                .putString(Param.RATE, rate.toJson())
                .apply()
        }

        fragment.viewLifecycleOwner.lifecycleScope.launch(handler + Dispatchers.IO) {

            val rate = sharedPreferences.getString(Param.RATE, "").toObjectOrNull<ReviewHomeViewModel.Rate>()
            viewModel.updateRate(rate)
        }
    }

    override fun showReview() {
        show.postDifferentValue(true)
    }

    private suspend fun openReview(fragment: HomeFragment) {

        val manager = ReviewManagerFactory.create(fragment.context ?: return)

        val reviewInfo = channelFlow {

            manager.requestReviewFlow().addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    trySend(task.result)
                    logAnalytics("app_review_request_success")
                } else {

                    trySend(null)
                    logCrashlytics("app_review_request_failed", task.exception ?: RuntimeException("not found error"))
                }
            }

            awaitClose {

            }
        }.firstOrNull()

        channelFlow {

            val flow = manager.launchReviewFlow(fragment.activity ?: return@channelFlow, reviewInfo ?: return@channelFlow)

            flow.addOnCompleteListener { it ->

                trySend(Unit)

                if (it.isSuccessful) {

                    logAnalytics("app_review_open_success")
                } else {

                    logCrashlytics("app_review_open_failed", it.exception ?: RuntimeException("not found error"))
                }
            }

            awaitClose {

            }
        }.firstOrNull()
    }

    private fun openStore(fragment: HomeFragment) = kotlin.runCatching {

        val context = fragment.context ?: return@runCatching

        val appPackageName = context.packageName // Package của ứng dụng

        if (isGooglePlayAvailable(context = context)) try {

            // Mở Google Play Store
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")));
        } catch (e: Exception) {

            // Dự phòng: mở trong trình duyệt
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")));
        } else try {

            // Mở cửa hàng ứng dụng thay thế (ví dụ: Huawei AppGallery)
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("appmarket://details?id=$appPackageName")));
        } catch (e: Exception) {

            // Dự phòng nếu không có cửa hàng nào: thông báo cho người dùng
        }
    }

    private fun isGooglePlayAvailable(context: Context): Boolean {

        val packageManager: PackageManager = context.packageManager

        try {

            packageManager.getPackageInfo("com.android.vending", 0) // "com.android.vending" là package của Google Play
            return true // Google Play Store có sẵn
        } catch (e: PackageManager.NameNotFoundException) {

            return false // Không tìm thấy Google Play Store
        }
    }
}