package com.simple.phonetics.ui.update

import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import com.google.auto.service.AutoService
import com.simple.analytics.logAnalytics
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.deeplink.sendDeeplink
import com.simple.event.listenerEvent
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.EventName
import com.simple.phonetics.Param
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.view.popup.PopupView
import com.simple.phonetics.ui.view.popup.PopupViewModel
import com.simple.phonetics.utils.exts.awaitResume
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val tag = "UPDATE"

@AutoService(PopupView::class)
class UpdateView : PopupView {

    override fun priority(): Int {
        return Int.MIN_VALUE
    }

    override fun setup(componentCallbacks: ComponentCallbacks) {

        if (componentCallbacks !is HomeFragment) return


        val viewModel: UpdateViewModel by componentCallbacks.viewModel()

        val eventViewModel: PopupViewModel by componentCallbacks.activityViewModel()


        eventViewModel.addEvent(key = tag)


        val show: LiveData<Boolean> = MediatorLiveData()

        viewModel.rateInfoEvent.asFlow().launchCollect(componentCallbacks.viewLifecycleOwner) { event ->

            show.asFlow().firstOrNull()

            val info = event.getContentIfNotHandled() ?: return@launchCollect

            val extras = mapOf(
                com.simple.coreapp.Param.CANCEL to false,

                com.simple.coreapp.Param.POSITIVE to info.positive,
                com.simple.coreapp.Param.NEGATIVE to info.negative,

                Param.KEY_REQUEST to tag,
                Param.VIEW_ITEM_LIST to info.viewItemList
            )

            if (info.show) {

                logAnalytics("open_update_confirm")
            }

            eventViewModel.addEvent(
                key = tag,
                index = priority(),
                deepLink = if (info.show) DeeplinkManager.UPDATE else "",

                extras = extras
            )
        }

        listenerEvent(componentCallbacks.viewLifecycleOwner.lifecycle, tag) {

            val result = it.asObjectOrNull<Int>() ?: 0

            if (result == 1) {
                openStore(componentCallbacks)
            }
        }

        listenerEvent(componentCallbacks.viewLifecycleOwner.lifecycle, EventName.SHOW_POPUP) {

            show.postDifferentValue(true)
        }
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
class UpdateDeeplinkHandler : DeeplinkHandler {

    override fun getDeeplink(): String {
        return DeeplinkManager.UPDATE
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is HomeFragment) {
            return false
        }


        val extrasWrap = extras.orEmpty().toMutableMap()

        val keyRequest = extrasWrap[Param.KEY_REQUEST].asObjectOrNull<String>() ?: tag.apply {

            extrasWrap[Param.KEY_REQUEST] = this
        }

        componentCallbacks.awaitResume()

        sendDeeplink(DeeplinkManager.CONFIRM + "code:update", extras = extrasWrap)


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