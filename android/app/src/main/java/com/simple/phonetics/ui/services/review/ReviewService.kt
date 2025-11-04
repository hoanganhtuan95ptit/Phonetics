package com.simple.phonetics.ui.services.review

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.edit
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.review.ReviewManagerFactory
import com.simple.analytics.logAnalytics
import com.simple.autobind.annotation.AutoBind
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.core.utils.extentions.toJson
import com.simple.core.utils.extentions.toObjectOrNull
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.crashlytics.logCrashlytics
import com.simple.deeplink.sendDeeplink
import com.simple.event.listenerEvent
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.PhoneticsApp
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.services.MainService
import com.simple.phonetics.ui.services.queue.QueueEventState
import com.simple.phonetics.ui.services.review.ReviewViewModel.RateInfo
import com.simple.state.ResultState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Calendar

private const val tag = "REVIEW"
private const val KEY_REQUEST = "${tag}_KEY_REQUEST"

private val sharedPreferences: SharedPreferences by lazy {

    PhoneticsApp.share.getSharedPreferences("AppReview", Context.MODE_PRIVATE)
}


@AutoBind(MainActivity::class)
class ReviewService : MainService {

    override fun setup(mainActivity: MainActivity) {

        val viewModel: ReviewViewModel by mainActivity.viewModel()


        QueueEventState.addTag(tag = tag)

        viewModel.rateInfo.asFlow().launchCollect(mainActivity) { data ->

            val state = if (data.show) {
                ResultState.Running(Unit)
            } else {
                ResultState.Success(Unit)
            }

            QueueEventState.updateState(tag = tag, state = state)
        }

        listenerEvent(mainActivity.lifecycle, tag) {

            val info = viewModel.rateInfo.asFlow().first()


            if (viewModel.historyList.value.orEmpty().isNotEmpty() && !info.show) {

                openReviewAwait(mainActivity)
            } else {

                openConfirmAwait(mainActivity = mainActivity, info = info)
            }

            QueueEventState.endTag(tag = tag, success = true)
        }


        mainActivity.lifecycleScope.launch(handler + Dispatchers.IO) {

            val rate = sharedPreferences.getString(Param.RATE, "").toObjectOrNull<ReviewViewModel.Rate>()
            viewModel.updateRate(rate)
        }
    }

    private suspend fun openReviewAwait(mainActivity: MainActivity) {

        val manager = ReviewManagerFactory.create(mainActivity)

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

            val flow = manager.launchReviewFlow(mainActivity, reviewInfo ?: return@channelFlow)

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

    private suspend fun openConfirmAwait(mainActivity: MainActivity, info: RateInfo) = channelFlow {

        listenerEvent(KEY_REQUEST) {

            val result = it.asObjectOrNull<Int>() ?: return@listenerEvent

            logAnalytics("rate_confirm_with_result_code_$result")

            if (result == 1) {

                openStoreAwait(mainActivity)
            }

            val rate = if (result == 1) ReviewViewModel.Rate(
                status = ReviewViewModel.Rate.Status.OPEN_RATE.value
            ) else ReviewViewModel.Rate(
                status = ReviewViewModel.Rate.Status.DISMISS.value,
                date = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            )

            sharedPreferences.edit {
                putString(Param.RATE, rate.toJson())
            }

            trySend(result)
        }


        val extras = mapOf(
            com.simple.coreapp.Param.CANCEL to false,

            com.simple.coreapp.Param.POSITIVE to info.positive,
            com.simple.coreapp.Param.NEGATIVE to info.negative,

            Param.KEY_REQUEST to KEY_REQUEST,
            Param.VIEW_ITEM_LIST to info.viewItemList
        )

        sendDeeplink(DeeplinkManager.CONFIRM + "code:review", extras = extras)

        awaitClose {

        }
    }.firstOrNull()

    private fun openStoreAwait(mainActivity: MainActivity) = runCatching {

        val context = mainActivity

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