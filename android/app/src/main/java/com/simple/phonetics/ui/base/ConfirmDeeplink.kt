package com.simple.phonetics.ui.base

import android.content.ComponentCallbacks
import android.view.View
import com.simple.phonetics.Deeplink
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.utils.appTheme
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
//
//        activity.supportFragmentManager.setFragmentResultListener(keyRequest, activity) { _, bundle ->
//
//            sendEvent(keyRequest, bundle)
//        }
//
//        val anchor = if (isCancel) Background(
//            backgroundColor = theme.colorOnBackgroundVariant,
//            cornerRadius = DP.DP_16,
//        ) else {
//            null
//        }
//
//        val fragment = VerticalConfirmDialogFragment.newInstance(
//            isCancel = isCancel,
//            keyRequest = keyRequest,
//
//            anchor = anchor,
//            background = Background(
//                backgroundColor = theme.colorBackground,
//                cornerRadius_TL = DP.DP_16,
//                cornerRadius_TR = DP.DP_16
//            )
//        )
//        fragment.arguments?.putAll(extras ?: bundleOf())
//        fragment.showOrAwaitDismiss(activity.supportFragmentManager, "")
//
//        sendEvent(EventName.DISMISS, bundleOf())

        return true
    }
}