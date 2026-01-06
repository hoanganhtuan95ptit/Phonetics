package com.simple.phonetics.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout

class InterceptConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var interceptListener: ((MotionEvent) -> Boolean)? = null

    /**
     * Đăng ký hàm lắng nghe khi onInterceptTouchEvent được gọi.
     * Nếu listener trả về true → view cha sẽ chặn sự kiện không cho con nhận.
     */
    fun setOnInterceptTouchListener(listener: (MotionEvent) -> Boolean) {
        interceptListener = listener
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Gọi listener nếu có
        val intercepted = interceptListener?.invoke(ev) ?: false

        // Nếu listener muốn chặn → return true
        if (intercepted) return true

        // Ngược lại → để super xử lý bình thường
        return super.onInterceptTouchEvent(ev)
    }
}


class InterceptCoordinatorLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CoordinatorLayout(context, attrs, defStyleAttr) {

    private var interceptListener: ((MotionEvent) -> Boolean)? = null

    /**
     * Đăng ký hàm lắng nghe khi onInterceptTouchEvent được gọi.
     * Nếu listener trả về true → view cha sẽ chặn sự kiện không cho con nhận.
     */
    fun setOnInterceptTouchListener(listener: (MotionEvent) -> Boolean) {
        interceptListener = listener
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Gọi listener nếu có
        val intercepted = interceptListener?.invoke(ev) ?: false

        // Nếu listener muốn chặn → return true
        if (intercepted) return true

        // Ngược lại → để super xử lý bình thường
        return super.onInterceptTouchEvent(ev)
    }
}
