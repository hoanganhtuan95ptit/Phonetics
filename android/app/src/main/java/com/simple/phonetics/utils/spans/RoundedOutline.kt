package com.simple.phonetics.utils.spans

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.ReplacementSpan
import androidx.annotation.Keep
import com.google.auto.service.AutoService
import com.simple.coreapp.utils.ext.RichSpan
import com.simple.coreapp.utils.ext.RichSpanConvert

data class RoundedOutline(
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
) : RichSpan()

@Keep
@AutoService(RichSpanConvert::class)
class RoundedOutlineSpanConvert : RichSpanConvert {

    override fun getAndroidSpan(richSpan: RichSpan): CharacterStyle? {
        return (richSpan as? RoundedOutline)?.let(::RoundedOutlineAndroidSpan)
    }
}

/**
 * Vẽ một khung viền bo góc bao quanh đoạn text.
 *
 * Layout theo chiều ngang:  [marginH][stroke + paddingH][TEXT][paddingH + stroke][marginH]
 * Layout theo chiều dọc:    [marginV][stroke + paddingV][TEXT][paddingV + stroke][marginV]
 *
 * Dùng [ReplacementSpan] thay vì [android.text.style.LineBackgroundSpan] để:
 *  - Tự kiểm soát text size khi đo và khi vẽ (không bị lệch khi có span khác như
 *    [android.text.style.AbsoluteSizeSpan]).
 *  - Có chỗ thật sự cho padding/margin ngang (LineBackgroundSpan không chừa được width).
 *  - Tăng line-height tự động qua [Paint.FontMetricsInt] để padding/margin dọc hoạt động.
 */
 class RoundedOutlineAndroidSpan(
    private val span: RoundedOutline
) : ReplacementSpan() {

    private val rect = RectF()
    private val path = Path()
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {

        val workPaint = workingPaint(paint)
        val textWidth = workPaint.measureText(text, start, end)

        // Tăng font metrics để layout chừa chỗ cho padding + margin chiều dọc.
        if (fm != null) {
            val textFm = workPaint.fontMetricsInt
            val extraVertical = (span.paddingVertical + span.marginVertical).toInt()

            fm.ascent = textFm.ascent - extraVertical
            fm.top = textFm.top - extraVertical
            fm.descent = textFm.descent + extraVertical
            fm.bottom = textFm.bottom + extraVertical
            fm.leading = textFm.leading
        }

        // Chừa chỗ cho padding + margin chiều ngang.
        val extraHorizontal = 2f * (span.paddingHorizontal + span.marginHorizontal)
        return (textWidth + extraHorizontal).toInt()
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

        val workPaint = workingPaint(paint)
        val textWidth = workPaint.measureText(text, start, end)
        val fm = workPaint.fontMetrics

        // Stroke vẽ tâm trên đường biên, lùi vào nửa stroke để không bị cắt mép.
        val halfStroke = span.strokeWidth / 2f
        val baseline = y.toFloat()

        // Viền: bắt đầu sau margin, kết thúc trước margin bên phải.
        val outlineLeft = x + span.marginHorizontal + halfStroke
        val outlineRight = x + span.marginHorizontal +
                span.paddingHorizontal * 2f + textWidth - halfStroke

        // Viền cao = text height + 2 * paddingVertical.
        val outlineTop = baseline + fm.ascent - span.paddingVertical + halfStroke
        val outlineBottom = baseline + fm.descent + span.paddingVertical - halfStroke

        rect.set(outlineLeft, outlineTop, outlineRight, outlineBottom)

        configureBackgroundPaint()

        if (span.dashWidth > 0f && span.dashGap > 0f) {
            // drawRoundRect không phải lúc nào cũng tôn trọng PathEffect khi bật
            // hardware acceleration — vẽ qua Path để DashPathEffect chắc chắn áp dụng.
            path.reset()
            path.addRoundRect(rect, span.cornerRadius, span.cornerRadius, Path.Direction.CW)
            canvas.drawPath(path, backgroundPaint)
        } else {
            canvas.drawRoundRect(
                rect,
                span.cornerRadius,
                span.cornerRadius,
                backgroundPaint
            )
        }

        // Vẽ text bên trong viền: lệch sang phải đúng marginHorizontal + paddingHorizontal.
        val textX = x + span.marginHorizontal + span.paddingHorizontal
        drawStyledText(canvas, text, start, end, textX, baseline, workPaint)
    }

    /**
     * Vẽ text trong khoảng [start, end] mà vẫn tôn trọng các [CharacterStyle] khác đang
     * phủ lên cùng range (ví dụ [android.text.style.ForegroundColorSpan]). Mặc định
     * [ReplacementSpan] sẽ "nuốt" hết các span con — phải đi từng đoạn và áp
     * `updateDrawState` cho paint trước khi vẽ thì các span đó mới có hiệu lực.
     */
    private fun drawStyledText(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        y: Float,
        basePaint: TextPaint
    ) {

        val spanned = text as? Spanned

        if (spanned == null) {
            canvas.drawText(text, start, end, x, y, basePaint)
            return
        }

        var cursorX = x
        var segStart = start

        while (segStart < end) {

            val segEnd = spanned.nextSpanTransition(segStart, end, CharacterStyle::class.java)

            val segPaint = TextPaint(basePaint)

            spanned.getSpans(segStart, segEnd, CharacterStyle::class.java)
                .filter { it !== this && it !is ReplacementSpan }
                .forEach { it.updateDrawState(segPaint) }

            // Trong khung viền, span.textSize là "chủ" — các MetricAffectingSpan khác
            // (AbsoluteSizeSpan, RelativeSizeSpan...) có thể vừa đổi textSize ở bước trên,
            // ép lại để chữ luôn vẽ đúng cỡ của RoundedOutline. Các thuộc tính khác
            // (màu, gạch chân, typeface...) vẫn giữ nguyên.
            if (span.textSize > 0f) {
                segPaint.textSize = span.textSize
            }

            canvas.drawText(text, segStart, segEnd, cursorX, y, segPaint)

            cursorX += segPaint.measureText(text, segStart, segEnd)
            segStart = segEnd
        }
    }

    private fun workingPaint(source: Paint): TextPaint {

        return TextPaint(source).apply {
            if (span.textSize > 0f) {
                textSize = span.textSize
            }
        }
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
