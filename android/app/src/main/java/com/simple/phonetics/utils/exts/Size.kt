package com.simple.phonetics.utils.exts

import android.content.res.Resources
import android.util.TypedValue

private fun Float.spToPx(): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this, Resources.getSystem().displayMetrics)
}

private fun Float.dpToPx(): Float {

    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics)
}

private val SP = HashMap<Float, Float>()

fun Int.sp() = toFloat().sp()

fun Float.sp() = SP[this] ?: this.spToPx().apply {
    SP[this@sp] = this
}


private val DP = HashMap<Float, Float>()

fun Int.dp() = toFloat().dp()

fun Float.dp() = DP[this] ?: this.dpToPx().apply {
    DP[this@dp] = this
}