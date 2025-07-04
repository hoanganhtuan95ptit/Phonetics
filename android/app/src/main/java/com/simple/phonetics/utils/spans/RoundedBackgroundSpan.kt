package com.simple.phonetics.utils.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.CharacterStyle
import android.text.style.ReplacementSpan
import com.google.auto.service.AutoService
import com.simple.coreapp.utils.ext.RichSpan
import com.simple.coreapp.utils.ext.RichSpanConvert


data class RoundedBackground(
    val backgroundColor: Int,
    val textColor: Int,
    val radius: Float = 20f
) : RichSpan()

@AutoService(RichSpanConvert::class)
class RelativeSizeConvert : RichSpanConvert {

    override fun getAndroidSpan(richSpan: RichSpan): CharacterStyle? {
        return if (richSpan is RoundedBackground) RoundedBackgroundSpan(richSpan.backgroundColor, richSpan.textColor, richSpan.radius) else null
    }
}

class RoundedBackgroundSpan(
    private val backgroundColor: Int,
    private val textColor: Int,
    private val radius: Float = 20f
) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        return (paint.measureText(text, start, end) + 2 * radius).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val textWidth = paint.measureText(text, start, end)
        val textHeight = paint.descent() - paint.ascent()

        val rectF = RectF(x, top.toFloat(), x + textWidth + 2 * radius, bottom.toFloat())
        paint.color = backgroundColor
        canvas.drawRoundRect(rectF, radius, radius, paint)

        paint.color = textColor
        canvas.drawText(text, start, end, x + radius, y.toFloat(), paint)
    }
}
