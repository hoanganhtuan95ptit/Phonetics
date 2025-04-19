package com.simple.phonetics.ui.base

import android.content.ComponentCallbacks
import android.view.View
import androidx.core.os.bundleOf
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.Param
import com.simple.coreapp.ui.dialogs.confirm.VerticalConfirmDialogFragment
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.phonetics.Deeplink
import com.simple.phonetics.EventName
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.utils.appTheme
import com.simple.phonetics.utils.sendEvent
import com.tuanha.deeplink.DeeplinkHandler
import kotlinx.coroutines.flow.firstOrNull

@com.tuanha.deeplink.annotation.Deeplink
class ConfirmDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return Deeplink.CONFIRM
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is MainActivity) return false

        val theme = appTheme.firstOrNull() ?: return false

        val isCancel = extras?.get(Param.CANCEL).asObjectOrNull<Boolean>() ?: false
        val keyRequest = extras?.get(Param.KEY_REQUEST).asObjectOrNull<String>().orEmpty()

        componentCallbacks.supportFragmentManager.setFragmentResultListener(keyRequest, componentCallbacks) { _, bundle ->

            sendEvent(keyRequest, bundle)
        }

        val anchor = if (isCancel) Background(
            backgroundColor = theme.colorOnBackgroundVariant,
            cornerRadius = DP.DP_16,
        ) else {
            null
        }

        val fragment = VerticalConfirmDialogFragment.newInstance(
            isCancel = isCancel,
            keyRequest = keyRequest,

            anchor = anchor,
            background = Background(
                backgroundColor = theme.colorBackground,
                cornerRadius_TL = DP.DP_16,
                cornerRadius_TR = DP.DP_16
            )
        )
        fragment.arguments = bundleOf(*extras?.toList().orEmpty().toTypedArray())
        fragment.showOrAwaitDismiss(componentCallbacks.supportFragmentManager, "")

        sendEvent(EventName.DISMISS, bundleOf())

        return true
    }
}