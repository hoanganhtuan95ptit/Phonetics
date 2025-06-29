package com.simple.phonetics.ui.review

import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.auto.service.AutoService
import com.simple.analytics.logAnalytics
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.core.utils.extentions.toJson
import com.simple.core.utils.extentions.toObjectOrNull
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.extentions.postValue
import com.simple.crashlytics.logCrashlytics
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.deeplink.sendDeeplink
import com.simple.event.listenerEvent
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.EventName
import com.simple.phonetics.Param
import com.simple.phonetics.PhoneticsApp
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.view.popup.PopupView
import com.simple.phonetics.ui.view.popup.PopupViewModel
import com.simple.phonetics.utils.exts.awaitResume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Calendar

private const val tag = "REVIEW"

@AutoService(PopupView::class)
class ReviewView : PopupView {

    override fun priority(): Int {
        return 1
    }

    override fun setup(componentCallbacks: ComponentCallbacks) {

        if (componentCallbacks !is HomeFragment) return


        val viewModel: ReviewViewModel by componentCallbacks.viewModel()

        val popupViewModel: PopupViewModel by componentCallbacks.activityViewModel()


        popupViewModel.addEvent(key = tag)


        val show: LiveData<Boolean> = MediatorLiveData()

        viewModel.rateInfoEvent.asFlow().launchCollect(componentCallbacks.viewLifecycleOwner) { event ->

            show.asFlow().firstOrNull()

            val info = event.getContentIfNotHandled() ?: return@launchCollect

            val extras = mapOf(
                com.simple.coreapp.Param.CANCEL to false,

                com.simple.coreapp.Param.POSITIVE to info.positive,
                com.simple.coreapp.Param.NEGATIVE to info.negative,

                Param.VIEW_ITEM_LIST to info.viewItemList
            )

            if (info.show) {

                logAnalytics("open_rate_confirm")
            }

            popupViewModel.addEvent(
                key = tag,
                index = 2,
                deepLink = if (info.show) DeeplinkManager.REVIEW else "",

                extras = extras
            )

            if (viewModel.historyList.value.orEmpty().isNotEmpty() && !info.show) {

                openReview(componentCallbacks)
            }
        }

        listenerEvent(componentCallbacks.viewLifecycleOwner.lifecycle, EventName.SHOW_POPUP) {

            show.postValue(true)
        }

        val sharedPreferences: SharedPreferences by lazy {

            PhoneticsApp.share.getSharedPreferences("AppReview", Context.MODE_PRIVATE)
        }

        listenerEvent(componentCallbacks.viewLifecycleOwner.lifecycle, tag) {

            val resultCode = it.asObjectOrNull<Int>() ?: return@listenerEvent

            logAnalytics("rate_confirm_with_result_code_$resultCode")

            if (resultCode == 1) {

                openStore(componentCallbacks)
            }

            val rate = if (resultCode == 1) ReviewViewModel.Rate(
                status = ReviewViewModel.Rate.Status.OPEN_RATE.value
            ) else ReviewViewModel.Rate(
                status = ReviewViewModel.Rate.Status.DISMISS.value,
                date = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            )

            sharedPreferences.edit()
                .putString(Param.RATE, rate.toJson())
                .apply()
        }

        componentCallbacks.viewLifecycleOwner.lifecycleScope.launch(handler + Dispatchers.IO) {

            val rate = sharedPreferences.getString(Param.RATE, "").toObjectOrNull<ReviewViewModel.Rate>()
            viewModel.updateRate(rate)
        }
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

@Deeplink(queue = "Confirm")
class ReviewDeeplinkHandler : DeeplinkHandler {

    override fun getDeeplink(): String {
        return DeeplinkManager.REVIEW
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is HomeFragment) {
            return false
        }


        val extrasWrap = extras.orEmpty().toMutableMap()

        val keyRequest = extrasWrap[Param.KEY_REQUEST].asObjectOrNull<String>() ?: tag.apply {

            extrasWrap[Param.KEY_REQUEST] = this
        }


        delay(350)
        componentCallbacks.awaitResume()


        sendDeeplink(DeeplinkManager.CONFIRM + "code:review", extras = extrasWrap)


        channelFlow {

            listenerEvent(eventName = keyRequest) {

                trySend(Unit)
            }

            awaitClose {

            }
        }.first()

        return true
    }
}