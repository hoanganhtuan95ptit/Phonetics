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
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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


fun View.listenerLayoutChangeAsync() = channelFlow {

    val onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        trySend(this@listenerLayoutChangeAsync)
    }

    viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)

    post {
        trySend(this@listenerLayoutChangeAsync)
    }

    awaitClose {
        viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
    }
}.asLiveData().asFlow()

fun View.listenerHeightChangeAsync() = listenerLayoutChangeAsync().map { it.height }.distinctUntilChanged()


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

fun View.doOnPreDrawAsync() = channelFlow {

    val timeoutJob = launch {

        delay(350)
        trySend(Unit)
    }

    doOnPreDraw {
        timeoutJob.cancel()
        trySend(Unit)
    }

    awaitClose {

    }
}