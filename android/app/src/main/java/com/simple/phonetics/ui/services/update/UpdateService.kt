package com.simple.phonetics.ui.services.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.asFlow
import com.simple.autobind.annotation.AutoBind
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.core.utils.extentions.orZero
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.deeplink.sendDeeplink
import com.simple.event.listenerEvent
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.services.MainService
import com.simple.phonetics.ui.services.queue.QueueEventState
import com.simple.state.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val tag = "UPDATE"
private const val KEY_REQUEST = "${tag}_KEY_REQUEST"

@AutoBind(MainActivity::class)
class UpdateView : MainService {

    override fun setup(mainActivity: MainActivity) {

        val viewModel: UpdateViewModel by mainActivity.viewModel()


        QueueEventState.addTag(tag = tag)

        viewModel.updateInfo.asFlow().launchCollect(mainActivity) { data ->

            val state = if (data.show) {
                ResultState.Running(Unit)
            } else {
                ResultState.Success(Unit)
            }

            QueueEventState.updateState(tag = tag, state = state)
        }

        listenerEvent(mainActivity.lifecycle, eventName = tag) {

            openConfirmAwait(mainActivity = mainActivity, info = viewModel.updateInfo.asFlow().first())

            QueueEventState.endTag(tag = tag)
        }
    }

    private suspend fun openConfirmAwait(mainActivity: MainActivity, info: UpdateViewModel.UpdateInfo) = channelFlow {


        listenerEvent(KEY_REQUEST) {

            val result = it.asObjectOrNull<Int>().orZero()

            if (result == 1) {
                openStore(mainActivity)
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

        sendDeeplink(DeeplinkManager.CONFIRM + "code:update", extras = extras)

        awaitClose {

        }
    }.firstOrNull()

    private fun openStore(mainActivity: MainActivity) = runCatching {

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