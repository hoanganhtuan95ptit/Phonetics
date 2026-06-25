package com.simple.phonetics.ui.view.outline

/**
 * Interface dùng chung cho mọi View muốn có outline bo góc.
 *
 * Class kế thừa chỉ cần:
 *  1. Override [outline] để khởi tạo delegate.
 *  2. Hook 4 lifecycle: onSizeChanged / onAttachedToWindow / onDetachedFromWindow / onDraw.
 *
 * Tất cả property và function bên dưới đã có default implementation — không cần viết lại.
 */
interface OutlineHost {

    val outline: OutlineDelegate

    // ---------- Cấu hình outline ----------

    var strokeColor: Int
        get() = outline.strokeColor
        set(value) { outline.strokeColor = value }

    var strokeWidth: Float
        get() = outline.strokeWidth
        set(value) { outline.strokeWidth = value }

    var cornerRadius: Float
        get() = outline.cornerRadius
        set(value) { outline.cornerRadius = value }

    var dashWidth: Float
        get() = outline.dashWidth
        set(value) { outline.dashWidth = value }

    var dashGap: Float
        get() = outline.dashGap
        set(value) { outline.dashGap = value }

    // ---------- Cấu hình loading ----------

    var loadingSegmentRatio: Float
        get() = outline.loadingSegmentRatio
        set(value) { outline.loadingSegmentRatio = value }

    var loadingDuration: Long
        get() = outline.loadingDuration
        set(value) { outline.loadingDuration = value }

    // ---------- State ----------

    val state: OutlineDelegate.State get() = outline.state

    fun setLoading(loading: Boolean, show: Boolean = true, animate: Boolean = true) =
        outline.setLoading(loading, show, animate)

    fun isLoading(): Boolean = outline.isLoading()

    fun isHidden(): Boolean = outline.isHidden()
}
