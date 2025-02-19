package com.simple.phonetics.ui.base

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.os.bundleOf
import com.simple.coreapp.Param
import com.simple.coreapp.ui.dialogs.confirm.VerticalConfirmDialogFragment
import com.simple.coreapp.ui.view.round.Background
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.phonetics.Deeplink
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.phonetics.utils.appTheme
import com.simple.phonetics.utils.sendEvent
import kotlinx.coroutines.flow.firstOrNull

@com.tuanha.deeplink.annotation.Deeplink
class ConfirmDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return Deeplink.CONFIRM
    }

    override suspend fun navigation(activity: ComponentActivity, deepLink: String, extras: Bundle?, sharedElement: Map<String, View>?): Boolean {

        if (activity !is MainActivity) return false

        val theme = appTheme.firstOrNull() ?: return false

        val isCancel = extras?.getBoolean(Param.CANCEL) ?: false
        val keyRequest = extras?.getString(Param.KEY_REQUEST).orEmpty()

        activity.supportFragmentManager.setFragmentResultListener(keyRequest, activity) { _, bundle ->

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
        fragment.arguments?.putAll(extras ?: bundleOf())
        fragment.showOrAwaitDismiss(activity.supportFragmentManager, "")

        return true
    }
}