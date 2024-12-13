package com.simple.phonetics.utils.exts

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources

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