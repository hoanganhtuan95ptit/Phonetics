package com.phonetics.size

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.widget.TextView
import androidx.startup.Initializer
import com.simple.phonetics.R
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

val map = hashMapOf<String, TextViewMetrics>()

val appStyle by lazy {

    MutableSharedFlow<Map<String, TextViewMetrics>>(replay = 1, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
}


class StyleInitializer : Initializer<Unit> {

    override fun create(context: Context) {

        if (context is Application) context.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityStarted(activity: Activity) {

                listOf(
                    com.simple.phonetics.R.style::class.java,
                ).flatMap {

                    it.fields.toList()
                }.forEach {

                    if (it.name.startsWith("Text", true)) kotlin.runCatching {

                        val textView = TextView(ContextThemeWrapper(activity, it.getInt(null)))

                        val style = TextViewMetrics(
                            typeface = textView.typeface,
                            textSizePx = textView.textSize,

                            includeFontPadding = textView.includeFontPadding,

                            lineSpacingExtra = textView.lineSpacingExtra,
                            lineSpacingMultiplier = textView.lineSpacingMultiplier,
                        )

                        map[it.name] = style
                    }
                }


                appStyle.tryEmit(map)
            }

            override fun onActivityResumed(activity: Activity) {
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
            }
        })
        return
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

data class TextViewMetrics(
    val textSizePx: Float,
    val typeface: Typeface?,
    val lineSpacingExtra: Float,
    val lineSpacingMultiplier: Float,
    val includeFontPadding: Boolean   // Thêm trường này
)
