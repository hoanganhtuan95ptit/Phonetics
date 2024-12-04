package com.simple.phonetics.utils

import android.app.Activity
import com.simple.coreapp.utils.extentions.getColorFromAttr
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

data class AppTheme(
    val primaryColor: Int,
    val primaryColorVariant: Int
)


val appTheme by lazy {

    MutableSharedFlow<AppTheme>(replay = 1, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
}

fun setupTheme(activity: Activity) {

    val theme = AppTheme(
        primaryColor = activity.getColorFromAttr(com.google.android.material.R.attr.colorPrimary),
        primaryColorVariant = activity.getColorFromAttr(com.google.android.material.R.attr.colorPrimaryVariant)
    )

    appTheme.tryEmit(theme)
}
