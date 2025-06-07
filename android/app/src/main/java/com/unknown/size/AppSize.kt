package com.unknown.size

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.extentions.getScreenHeight
import com.simple.coreapp.utils.extentions.getScreenWidth
import com.unknown.size.provider.SizeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap

data class AppSize(
    val width: Int = 0,
    val height: Int = 0,

    val heightStatusBar: Int = 0,
    val heightNavigationBar: Int = 0
) : ConcurrentHashMap<String, Int>()


val appSize by lazy {

    MutableSharedFlow<AppSize>(replay = 1, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
}

fun setupSize(activity: FragmentActivity) = activity.lifecycleScope.launch(handler + Dispatchers.IO) {

    val size = AppSize(
        width = getScreenWidth(activity),
        height = getScreenHeight(activity)
    )

    ServiceLoader.load(SizeProvider::class.java).map { provider ->

        provider.provide(activity).launchCollect(this) {

            size.putAll(it)
            appSize.tryEmit(size)
        }
    }
}