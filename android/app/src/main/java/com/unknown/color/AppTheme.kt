package com.unknown.color

import android.graphics.Color
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.R
import com.simple.coreapp.utils.ext.handler
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.extentions.getColorFromAttr
import com.unknown.color.provider.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap


data class AppTheme(
    val isDarkMode: Boolean = false,

    val colorPrimary: Int,
    val colorOnPrimary: Int,
    val colorPrimaryVariant: Int,
    val colorOnPrimaryVariant: Int,

    val colorSurface: Int,

    val colorOnSurface: Int,
    val colorOnSurfaceVariant: Int,

    val colorError: Int,
    val colorOnError: Int,

    val colorErrorVariant: Int,
    val colorOnErrorVariant: Int,

    val colorDivider: Int,
    val colorLoading: Int,

    val colorBackground: Int,
    val colorOnBackground: Int,

    val colorBackgroundVariant: Int,
    val colorOnBackgroundVariant: Int,

    val vowelsLong: Int = Color.parseColor("#FCE9DA"),
    val vowelsShort: Int = Color.parseColor("#FEF3ED"),
    val consonantsVoiced: Int = Color.parseColor("#92B2D8"),
    val consonantsUnvoiced: Int = Color.parseColor("#FCE9DA"),
    val diphthongs: Int = Color.parseColor("#E4B795"),
) : ConcurrentHashMap<String, Int>()


val appTheme by lazy {

    MutableSharedFlow<AppTheme>(replay = 1, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
}

fun setupColor(activity: FragmentActivity) = activity.lifecycleScope.launch(handler + Dispatchers.IO) {

    val theme = AppTheme(
        colorPrimary = activity.getColorFromAttr(R.attr.colorPrimary),
        colorOnPrimary = activity.getColorFromAttr(R.attr.colorOnPrimary),
        colorPrimaryVariant = activity.getColorFromAttr(R.attr.colorPrimaryVariant),
        colorOnPrimaryVariant = activity.getColorFromAttr(com.simple.coreapp.R.attr.colorOnPrimaryVariant),

        colorDivider = activity.getColorFromAttr(com.simple.coreapp.R.attr.colorDivider),

        colorSurface = activity.getColorFromAttr(R.attr.colorSurface),
        colorOnSurface = activity.getColorFromAttr(R.attr.colorOnSurface),
        colorOnSurfaceVariant = activity.getColorFromAttr(R.attr.colorOnSurfaceVariant),

        colorError = Color.parseColor("#E9201F"),
        colorOnError = Color.parseColor("#FFFFFF"),

        colorErrorVariant = Color.parseColor("#FFDFE2"),
        colorOnErrorVariant = Color.parseColor("#E9201F"),

        colorLoading = Color.parseColor("#D1D2D4"),

        colorBackground = activity.getColorFromAttr(android.R.attr.colorBackground),
        colorOnBackground = activity.getColorFromAttr(R.attr.colorOnBackground),

        colorBackgroundVariant = activity.getColorFromAttr(com.simple.coreapp.R.attr.colorBackgroundVariant),
        colorOnBackgroundVariant = activity.getColorFromAttr(com.simple.coreapp.R.attr.colorOnBackgroundVariant),
    )

    ServiceLoader.load(ColorProvider::class.java).map { provider ->

        provider.provide(activity).launchCollect(this) {

            theme.putAll(it)
            appTheme.tryEmit(theme)
        }
    }
}
