package com.simple.phonetics.utils

import android.view.View

fun sendDeeplinkWithThank(deepLink: String, extras: Map<String, Any?>? = null, sharedElement: Map<String, View>? = null) {

    com.simple.deeplink.sendDeeplink(deepLink, extras, sharedElement)
    com.simple.deeplink.sendDeeplink(deepLink.replace("app:", "thank:"))
}