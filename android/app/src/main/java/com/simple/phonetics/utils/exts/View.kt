package com.simple.phonetics.utils.exts

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged

fun ImageView.setImageDrawable(context: Context, resId: Int, color: Int) {

    val drawable = AppCompatResources.getDrawable(context, resId)
    drawable!!.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    setImageDrawable(drawable)
}

fun TextView.setTextColor(vararg pair: Pair<IntArray, Int>) {

    setTextColor(
        ColorStateList(
            pair.toMap().keys.toTypedArray(),
            pair.toMap().values.toIntArray()
        )
    )
}

fun View.listenerOnHeightChange() = channelFlow {

    val onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        trySend(height)
    }

    viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)

    awaitClose {
        viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
    }
}.distinctUntilChanged()

fun View.ensureGradientUpdates() = channelFlow<Unit> {

    val view = this@ensureGradientUpdates

    val onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {

        (view.background as? GradientDrawable)?.setBounds(0, 0, view.width, view.height)
    }

    viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)

    awaitClose {
        viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
    }
}