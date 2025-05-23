package com.simple.phonetics.ui.base

import android.content.ComponentCallbacks
import android.view.View
import androidx.core.os.bundleOf
import com.simple.coreapp.ui.dialogs.ToastDialog
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.ui.MainActivity

@Deeplink
class ToastDeeplink : DeeplinkHandler {

    override suspend fun acceptDeeplink(deepLink: String): Boolean {
        return deepLink.startsWith(DeeplinkManager.TOAST, true)
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is MainActivity) return false

        val fragment = ToastDialog.newInstance()
        fragment.arguments = bundleOf(*extras?.toList().orEmpty().toTypedArray())
        fragment.showOrAwaitDismiss(componentCallbacks.supportFragmentManager, "")

        return true
    }
}