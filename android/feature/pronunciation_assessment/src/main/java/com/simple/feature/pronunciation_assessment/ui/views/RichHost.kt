package com.simple.feature.pronunciation_assessment.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import com.simple.coreapp.utils.ext.RichText

interface DrawView {

    val frame: Rect

    fun onDraw(canvas: Canvas)
}

private val TEXT_PAINT = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = 12f
    color = Color.BLACK
}

data class RichData(
    override val frame: Rect,
    val gravity: Int,
    val richText: RichText
) : DrawView {

    override fun onDraw(canvas: Canvas) {

        val text = richText.text
        if (text.isEmpty() || frame.width() <= 0) return

        // Dùng StaticLayout để text tự xuống dòng theo width của frame,
        // và sau này dễ mở rộng cho spans (SpannableString).
        val layout: StaticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, TEXT_PAINT, frame.width())
            .setAlignment(gravity.toLayoutAlignment())
            .setIncludePad(false)
            .setLineSpacing(0f, 1f)
            .build()

        val layoutWidth = layout.width.toFloat()
        val layoutHeight = layout.height.toFloat()

        // Toạ độ X theo horizontal gravity
        val dx = when (gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
            Gravity.CENTER_HORIZONTAL -> frame.left + (frame.width() - layoutWidth) / 2f
            Gravity.RIGHT, Gravity.END -> frame.right - layoutWidth
            else -> frame.left.toFloat()
        }

        // Toạ độ Y theo vertical gravity
        val dy = when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
            Gravity.CENTER_VERTICAL -> frame.top + (frame.height() - layoutHeight) / 2f
            Gravity.BOTTOM -> frame.bottom - layoutHeight
            else -> frame.top.toFloat()
        }

        val saveCount = canvas.save()
        canvas.translate(dx, dy)
        layout.draw(canvas)
        canvas.restoreToCount(saveCount)
    }

    private fun Int.toLayoutAlignment(): Layout.Alignment = when (this and Gravity.HORIZONTAL_GRAVITY_MASK) {
        Gravity.CENTER_HORIZONTAL -> Layout.Alignment.ALIGN_CENTER
        Gravity.RIGHT, Gravity.END -> Layout.Alignment.ALIGN_OPPOSITE
        else -> Layout.Alignment.ALIGN_NORMAL
    }
}


class TextRich @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var rich: RichData? = null
        set(value) {
            field = value
            invalidate()
        }

    init {
        // FrameLayout mặc định không gọi onDraw, phải bật flag này
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        rich?.onDraw(canvas)
    }
}
