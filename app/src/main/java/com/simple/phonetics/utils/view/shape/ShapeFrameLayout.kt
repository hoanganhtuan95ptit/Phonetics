package com.simple.phonetics.utils.view.shape

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.FrameLayout


class ShapeFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs), ShapeOfView {

    override val path = Path()

    override var rectF: RectF = RectF()

    override var topLeftRadius = 0f
    override var topRightRadius = 0f
    override var bottomRightRadius = 0f
    override var bottomLeftRadius = 0f


    private val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        refresh()
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        canvas?.drawPath(path, clipPaint)
    }
}