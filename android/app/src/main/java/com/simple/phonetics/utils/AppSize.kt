package com.simple.phonetics.utils

import android.app.Activity
import com.simple.coreapp.utils.ext.doOnHeightStatusAndHeightNavigationChange
import com.simple.coreapp.utils.extentions.getScreenHeight
import com.simple.coreapp.utils.extentions.getScreenWidth
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

data class AppSize(
    val width: Int = 0,
    val height: Int = 0,

    val heightStatusBar: Int = 0,
    val heightNavigationBar: Int = 0
)


val appSize by lazy {

    MutableSharedFlow<AppSize>(replay = 1, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
}

fun setupSize(activity: Activity) {

    val size = AppSize(
        width = getScreenWidth(activity),
        height = getScreenHeight(activity)
    )

    appSize.tryEmit(size)

    activity.doOnHeightStatusAndHeightNavigationChange { heightStatusBar, heightNavigationBar ->

        appSize.tryEmit(
            size.copy(
                heightStatusBar = heightStatusBar,
                heightNavigationBar = heightNavigationBar
            )
        )
    }
}