package com.simple.phonetics.utils.exts

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan


fun String.boldWith(bold: String): CharSequence {

    return (this as CharSequence).with(bold, StyleSpan(Typeface.BOLD))
}

fun String.with(bold: String, vararg spannable: Any): CharSequence {

    val spannableString = SpannableString(this)

    val start = indexOf(bold)
    if (start < 0) return spannableString

    val end = start + bold.length
    if (end > length) return spannableString

    spannable.forEach {

        spannableString.setSpan(it, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    return spannableString
}

fun CharSequence.with(bold: String, vararg spannable: Any): CharSequence {

    val spannableString = SpannableString(this)

    val start = indexOf(bold)
    if (start < 0) return spannableString

    val end = start + bold.length
    if (end > length) return spannableString

    spannable.forEach {

        spannableString.setSpan(it, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    return spannableString
}