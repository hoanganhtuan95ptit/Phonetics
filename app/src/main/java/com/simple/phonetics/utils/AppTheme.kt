package com.simple.phonetics.utils

import android.app.Activity
import android.view.View
import com.simple.coreapp.utils.extentions.getColorFromAttr
import com.simple.phonetics.R
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

data class AppTheme(
    val isDarkMode: Boolean = false,

    val colorPrimary: Int,
    val colorOnPrimary: Int,
    val colorPrimaryVariant: Int,
    val colorOnPrimaryVariant: Int,

    val colorSurface: Int,

    val colorOnSurface: Int ,
    val colorOnSurfaceVariant: Int,

    val colorError: Int,
    val colorDivider: Int,
    val colorLoading: Int,

    val colorBackground: Int,
    val colorOnBackground: Int,

    val colorBackgroundVariant: Int,
    val colorOnBackgroundVariant: Int,
)


val appTheme by lazy {

    MutableSharedFlow<AppTheme>(replay = 1, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
}

fun setupTheme(activity: Activity) {

    val theme = AppTheme(
        colorPrimary = activity.getColorFromAttr(com.google.android.material.R.attr.colorPrimary),
        colorOnPrimary = activity.getColorFromAttr(com.google.android.material.R.attr.colorOnPrimary),
        colorPrimaryVariant = activity.getColorFromAttr(com.google.android.material.R.attr.colorPrimaryVariant),
        colorOnPrimaryVariant = activity.getColorFromAttr(com.simple.coreapp.R.attr.colorOnPrimaryVariant),

        colorDivider = activity.getColorFromAttr(com.simple.coreapp.R.attr.colorDivider),

        colorSurface = activity.getColorFromAttr(com.google.android.material.R.attr.colorSurface),
        colorOnSurface = activity.getColorFromAttr(com.google.android.material.R.attr.colorOnSurface),
        colorOnSurfaceVariant = activity.getColorFromAttr(com.google.android.material.R.attr.colorOnSurfaceVariant),

        colorError = activity.getColorFromAttr(com.google.android.material.R.attr.colorError),

        colorLoading = activity.getColorFromAttr(com.simple.coreapp.R.attr.colorLoading),

        colorBackground = activity.getColorFromAttr(android.R.attr.colorBackground),
        colorOnBackground = activity.getColorFromAttr(com.google.android.material.R.attr.colorOnBackground),

        colorBackgroundVariant = activity.getColorFromAttr(com.simple.coreapp.R.attr.colorBackgroundVariant),
        colorOnBackgroundVariant = activity.getColorFromAttr(com.simple.coreapp.R.attr.colorOnBackgroundVariant),
    )

    appTheme.tryEmit(theme)
}

fun changeTheme(activity: Activity) {

    activity.setTheme(R.style.Theme_Phonetics_Dark)
    activity.window.decorView.systemUiVisibility = 0

    setupTheme(activity)
}
