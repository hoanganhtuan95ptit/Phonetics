package com.simple.phonetics.ui.base

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.os.bundleOf
import com.simple.coreapp.ui.dialogs.ToastDialog
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.phonetics.Deeplink
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.utils.DeeplinkHandler

@com.tuanha.deeplink.annotation.Deeplink
class ToastDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return Deeplink.TOAST
    }

    override suspend fun navigation(activity: ComponentActivity, deepLink: String, extras: Bundle?, sharedElement: Map<String, View>?): Boolean {

        if (activity !is MainActivity) return false

        val fragment = ToastDialog.newInstance()
        fragment.arguments?.putAll(extras ?: bundleOf())
        fragment.showOrAwaitDismiss(activity.supportFragmentManager, "")

        return true
    }
}