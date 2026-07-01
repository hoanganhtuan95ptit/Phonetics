package com.simple.phonetics.utils.spans

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.LineBackgroundSpan
import android.text.style.MetricAffectingSpan
import androidx.annotation.Keep
import com.google.auto.service.AutoService
import com.simple.ui.precompute.text.BigImageSpan
import com.simple.ui.precompute.text.BigImageSpanConvert

data class BigRoundedOutline(
    val textSize: Float,
    val paddingHorizontal: Float = 0f,
    val paddingVertical: Float = 0f,
    val marginHorizontal: Float = 0f,
    val marginVertical: Float = 0f,
    val strokeColor: Int,
    val strokeWidth: Float = 1f,
    val cornerRadius: Float = 0f,
    val dashWidth: Float = 0f,
    val dashGap: Float = 0f
) : BigImageSpan

@Keep
@AutoService(BigImageSpanConvert::class)
class BigRoundedOutlineSpanConvert : BigImageSpanConvert {

    override fun convert(bigSpan: BigImageSpan): CharacterStyle? {
        return (bigSpan as? BigRoundedOutline)?.let(::RoundedOutlineAndroidSpan)
    }
}

private class RoundedOutlineAndroidSpan(
    private val span: BigRoundedOutline
) : MetricAffectingSpan(), LineBackgroundSpan {

    private val rect = RectF()

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun updateMeasureState(paint: TextPaint) {
        applyVerticalSpacing(paint)
    }

    override fun updateDrawState(tp: TextPaint) = Unit

    private fun applyVerticalSpacing(paint: TextPaint) {

        val fontMetrics = Paint.FontMetrics()

        TextPaint(paint).apply {
            textSize = span.textSize
        }.getFontMetrics(fontMetrics)

        val originalHeight = fontMetrics.descent - fontMetrics.ascent

        val targetHeight = originalHeight +
                (span.paddingVertical * 2) +
                (span.marginVertical * 2)

        paint.textSize = span.textSize * (targetHeight / originalHeight)
    }

    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lineNumber: Int
    ) {

        val spanned = text as? Spanned ?: return

        val range = resolveLineSpanRange(
            spanned = spanned,
            lineStart = start,
            lineEnd = end
        ) ?: return

        val measurePaint = createMeasurePaint(paint)

        val rect = buildOutlineRect(
            measurePaint = measurePaint,
            left = left,
            baseline = baseline,
            text = text,
            lineStart = start,
            spanStart = range.first,
            spanEnd = range.last + 1
        )

        configureBackgroundPaint()

        canvas.drawRoundRect(
            rect,
            span.cornerRadius,
            span.cornerRadius,
            backgroundPaint
        )
    }

    private fun resolveLineSpanRange(
        spanned: Spanned,
        lineStart: Int,
        lineEnd: Int
    ): IntRange? {

        val spanStart = spanned.getSpanStart(this)
        val spanEnd = spanned.getSpanEnd(this)

        val resolvedStart = spanStart.coerceAtLeast(lineStart)
        val resolvedEnd = spanEnd.coerceAtMost(lineEnd)

        return if (resolvedStart < resolvedEnd) {
            resolvedStart until resolvedEnd
        } else {
            null
        }
    }

    private fun createMeasurePaint(source: Paint) = TextPaint(source).apply {
        textSize = span.textSize
    }

    private fun buildOutlineRect(
        measurePaint: TextPaint,
        left: Int,
        baseline: Int,
        text: CharSequence,
        lineStart: Int,
        spanStart: Int,
        spanEnd: Int
    ): RectF {

        val textBeforeWidth = measurePaint.measureText(
            text,
            lineStart,
            spanStart
        )

        val spanWidth = measurePaint.measureText(
            text,
            spanStart,
            spanEnd
        )

        val fontMetrics = measurePaint.fontMetrics

        val spanLeft = left +
                textBeforeWidth +
                span.marginHorizontal

        val spanRight = spanLeft + spanWidth

        val spanTop = baseline +
                fontMetrics.ascent -
                span.paddingVertical

        val spanBottom = baseline +
                fontMetrics.descent +
                span.paddingVertical

        rect.set(
            spanLeft - span.paddingHorizontal,
            spanTop,
            spanRight + span.paddingHorizontal,
            spanBottom
        )

        return rect
    }

    private fun configureBackgroundPaint() {

        backgroundPaint.apply {

            style = Paint.Style.STROKE

            color = span.strokeColor

            strokeWidth = span.strokeWidth

            pathEffect = if (
                span.dashWidth > 0f &&
                span.dashGap > 0f
            ) {
                DashPathEffect(
                    floatArrayOf(span.dashWidth, span.dashGap),
                    0f
                )
            } else {
                null
            }
        }
    }
}
