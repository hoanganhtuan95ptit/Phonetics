package com.simple.phonetics.ui.deeplinks

import android.content.ComponentCallbacks
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.util.Patterns
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.phonetics.utils.exts.awaitResume

@Deeplink("Web")
class WebDeeplink : DeeplinkHandler {

    override suspend fun acceptDeeplink(deepLink: String): Boolean {
        return Patterns.WEB_URL.matcher(deepLink).matches()
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is FragmentActivity) {

            return false
        }

        componentCallbacks.awaitResume()

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
        componentCallbacks.startActivity(intent)

        return true
    }
}