package com.simple.phonetics.ui.view.outline

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 * View vẽ đường outline bo góc, hỗ trợ nét đứt và 3 trạng thái.
 * Kế thừa [LinearLayout]. Logic vẽ/animation nằm ở [OutlineDelegate].
 *
 * @see OutlineFrameLayout — phiên bản kế thừa FrameLayout.
 */
open class OutlineLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), OutlineHost {

    override val outline = OutlineDelegate(this, context, attrs)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        outline.onSizeChanged(w, h)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        outline.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        outline.onDetachedFromWindow()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        outline.onDraw(canvas)
    }
}
