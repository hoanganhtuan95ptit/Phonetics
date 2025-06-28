package com.simple.phonetics.ui.deeplinks

import android.content.ComponentCallbacks
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.utils.Utils
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param

@Deeplink("action")
class CopyDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return DeeplinkManager.COPY
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is FragmentActivity) {
            return false
        }

        Utils.copyText(componentCallbacks, extras.orEmpty()[Param.TEXT].asObjectOrNull<String>().orEmpty())

        return true
    }
}