package com.simple.phonetics.utils.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.CharacterStyle
import android.text.style.ReplacementSpan
import androidx.annotation.Keep
import com.google.auto.service.AutoService
import com.simple.ui.precompute.text.BigSpan
import com.simple.ui.precompute.text.BigSpanConvert


data class BigRoundedBackground(
    val backgroundColor: Int,
    val textColor: Int,
    val radius: Float = 20f
) : BigSpan()

@Keep
@AutoService(BigSpanConvert::class)
class BigRoundedBackgroundConvert : BigSpanConvert {

    override fun getAndroidSpan(bigSpan: BigSpan): CharacterStyle? {
        return if (bigSpan is BigRoundedBackground) RoundedBackgroundSpan(bigSpan.backgroundColor, bigSpan.textColor, bigSpan.radius) else null
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
