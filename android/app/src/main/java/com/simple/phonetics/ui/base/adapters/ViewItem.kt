package com.simple.phonetics.ui.base.adapters

import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.ViewGroup
import com.simple.coreapp.ui.view.Size
import com.simple.phonetics.utils.TextViewMetrics
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min


interface SizeViewItem {

    var size: Size

    fun measure(appSize: Map<String, Int>, style: Map<String, TextViewMetrics>) {

        if (size.height != ViewGroup.LayoutParams.WRAP_CONTENT && size.width != ViewGroup.LayoutParams.WRAP_CONTENT) {

            return
        }

        size = measureSize(appSize, style)
    }

    fun measureSize(appSize: Map<String, Int>, style: Map<String, TextViewMetrics>): Size {

        return size
    }
}

fun measureTextViewWidth(
    text: CharSequence,
    maxWidth: Int,
    metrics: TextViewMetrics
): Int {

    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = metrics.textSizePx
        setTypeface(metrics.typeface)
        textScaleX = metrics.textScaleX
        letterSpacing = metrics.letterSpacing
    }

    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(metrics.lineSpacingExtra, metrics.lineSpacingMultiplier)
        .setIncludePad(metrics.includeFontPadding)
        .build()

    var maxLineWidth = 0f
    for (i in 0 until layout.lineCount) {
        maxLineWidth = max(maxLineWidth, layout.getLineWidth(i))
    }

    return ceil(maxLineWidth).toInt()
}

fun measureTextViewHeight(
    text: CharSequence,
    maxWidth: Int,
    metrics: TextViewMetrics,
    maxLines: Int? = null
): Int {

    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = metrics.textSizePx
        setTypeface(metrics.typeface)
        textScaleX = metrics.textScaleX
        letterSpacing = metrics.letterSpacing
    }

    val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, maxWidth)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(metrics.lineSpacingExtra, metrics.lineSpacingMultiplier)
        .setIncludePad(metrics.includeFontPadding)
        .build()

    val maxLines = maxLines ?: Int.MAX_VALUE
    val lineCount = min(staticLayout.lineCount, maxLines)

    // Nếu lineCount = 0 → không có gì để đo
    if (lineCount == 0) return 0

    val height = staticLayout.getLineBottom(lineCount - 1)

    return height
}