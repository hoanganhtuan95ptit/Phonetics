package com.simple.phonetics.ui.common

import android.content.ComponentCallbacks
import android.view.View
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.Param
import com.simple.coreapp.ui.dialogs.toast.ToastDialog
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.ui.main.MainActivity

@Deeplink
class ToastDeeplink : DeeplinkHandler {

    override suspend fun acceptDeeplink(deepLink: String): Boolean {
        return deepLink.startsWith(DeeplinkManager.TOAST, true)
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is MainActivity) {
            return false
        }

        val message = extras.orEmpty()[Param.MESSAGE].asObjectOrNull<RichText>()

        val background = extras.orEmpty()[Param.BACKGROUND].asObjectOrNull<Background>()

        val fragment = ToastDialog.newInstance(componentCallbacks, message = message, background = background)
        fragment.showOrAwaitDismiss(componentCallbacks.supportFragmentManager, "")

        return true
    }
}