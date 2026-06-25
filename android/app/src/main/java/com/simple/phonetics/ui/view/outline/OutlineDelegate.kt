package com.simple.phonetics.ui.view.outline

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.simple.phonetics.R

/**
 * Delegate chứa toàn bộ logic vẽ outline bo góc, hỗ trợ nét đứt và 3 trạng thái.
 *
 * Dùng chung cho [OutlineLinearLayout] (kế thừa LinearLayout) và [OutlineFrameLayout] (kế thừa FrameLayout).
 * Mỗi View chỉ cần:
 *  1. Tạo instance `OutlineDelegate(this, context, attrs)` trong constructor.
 *  2. Gọi `outline.onSizeChanged / onAttachedToWindow / onDetachedFromWindow / onDraw` trong các override tương ứng.
 *  3. Expose các property/function cần thiết ra ngoài.
 */
class OutlineDelegate(private val view: View, context: Context, attrs: AttributeSet?) {

    enum class State { IDLE, LOADING, HIDDEN }

    /**
     *  - [SHRINKING]: đuôi tiến (head đứng yên) ⇒ segLen giảm dần về [targetSegLen].
     *  - [GROWING]  : đầu tiến (tail đứng yên) ⇒ segLen tăng dần đến [targetSegLen].
     *  - [LOADING]  : cả hai cùng tiến với tốc độ chuẩn ⇒ segLen giữ nguyên.
     */
    private enum class InternalState { IDLE, LOADING, HIDDEN, SHRINKING, GROWING }

    // ---------- Cấu hình outline ----------

    var strokeColor: Int = Color.BLACK
        set(value) {
            field = value
            paint.color = value
            view.invalidate()
        }

    var strokeWidth: Float = 1f
        set(value) {
            field = value
            paint.strokeWidth = value
            updatePath()
            view.invalidate()
        }

    var cornerRadius: Float = 0f
        set(value) {
            field = value
            updatePath()
            view.invalidate()
        }

    var dashWidth: Float = 0f
        set(value) {
            field = value
            updateDashEffect()
            view.invalidate()
        }

    var dashGap: Float = 0f
        set(value) {
            field = value
            updateDashEffect()
            view.invalidate()
        }

    // ---------- Cấu hình loading ----------

    var loadingSegmentRatio: Float = 0.5f
        set(value) {
            field = value.coerceIn(0.05f, 1f)
            view.invalidate()
        }

    var loadingDuration: Long = 1200L
        set(value) {
            field = value.coerceAtLeast(50L)
        }

    var state: State = State.IDLE
        private set

    // ---------- Nội bộ ----------

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fullPath = Path()
    private val segmentPath = Path()
    private val pathMeasure = PathMeasure()
    private var pathLength: Float = 0f

    private var internalState: InternalState = InternalState.IDLE
    private var settledInternalState: InternalState = InternalState.IDLE
    private var tailPos: Float = 0f
    private var segLen: Float = 1f
    private var targetSegLen: Float = 1f

    private var animator: ValueAnimator? = null
    private var lastFrameMs: Long = 0L

    init {
        // ViewGroup mặc định không gọi onDraw() để tối ưu — bật lại để vẽ outline.
        view.setWillNotDraw(false)

        paint.color = strokeColor
        paint.strokeWidth = strokeWidth

        attrs?.let { readAttrs(context, it) }
    }

    private fun readAttrs(context: Context, attrs: AttributeSet) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.OutlineView)
        try {
            strokeColor = a.getColor(R.styleable.OutlineView_ov_strokeColor, strokeColor)
            strokeWidth = a.getDimension(R.styleable.OutlineView_ov_strokeWidth, strokeWidth)
            cornerRadius = a.getDimension(R.styleable.OutlineView_ov_cornerRadius, cornerRadius)
            dashWidth = a.getDimension(R.styleable.OutlineView_ov_dashWidth, dashWidth)
            dashGap = a.getDimension(R.styleable.OutlineView_ov_dashGap, dashGap)
            loadingSegmentRatio = a.getFloat(
                R.styleable.OutlineView_ov_loadingSegmentRatio,
                loadingSegmentRatio
            )
            loadingDuration = a.getInt(
                R.styleable.OutlineView_ov_loadingDuration,
                loadingDuration.toInt()
            ).toLong()
            val initialLoading = a.getBoolean(R.styleable.OutlineView_ov_loading, false)
            val initialShow = a.getBoolean(R.styleable.OutlineView_ov_show, true)
            setLoading(loading = initialLoading, show = initialShow, animate = false)
        } finally {
            a.recycle()
        }
    }

    // ---------- API ----------

    @JvmOverloads
    fun setLoading(loading: Boolean, show: Boolean = true, animate: Boolean = true) {
        val target: State = when {
            !show -> State.HIDDEN
            loading -> State.LOADING
            else -> State.IDLE
        }
        state = target

        val targetLen: Float = when (target) {
            State.HIDDEN -> 0f
            State.LOADING -> loadingSegmentRatio
            State.IDLE -> 1f
        }
        val settled: InternalState = when (target) {
            State.HIDDEN -> InternalState.HIDDEN
            State.LOADING -> InternalState.LOADING
            State.IDLE -> InternalState.IDLE
        }

        targetSegLen = targetLen
        settledInternalState = settled

        if (!animate) {
            segLen = targetLen
            internalState = settled
            if (needsAnimating(settled)) startAnimating() else stopAnimating()
            view.invalidate()
            return
        }

        internalState = when {
            segLen > targetLen + EPS -> InternalState.SHRINKING
            segLen < targetLen - EPS -> InternalState.GROWING
            else -> settled
        }

        if (needsAnimating(internalState)) startAnimating() else {
            stopAnimating()
            view.invalidate()
        }
    }

    fun isLoading(): Boolean = state == State.LOADING

    fun isHidden(): Boolean = state == State.HIDDEN

    // ---------- Lifecycle hooks — gọi từ View ----------

    fun onSizeChanged(w: Int, h: Int) {
        updatePath(w.toFloat(), h.toFloat())
    }

    fun onAttachedToWindow() {
        if (needsAnimating(internalState)) startAnimating()
    }

    fun onDetachedFromWindow() {
        stopAnimating()
    }

    fun onDraw(canvas: Canvas) {
        if (pathLength <= 0f) return
        if (internalState == InternalState.HIDDEN || segLen <= 0f) return

        if (segLen >= 1f) {
            canvas.drawPath(fullPath, paint)
            return
        }

        val start = tailPos * pathLength
        val end = start + segLen * pathLength

        segmentPath.reset()
        if (end <= pathLength) {
            pathMeasure.getSegment(start, end, segmentPath, true)
        } else {
            pathMeasure.getSegment(start, pathLength, segmentPath, true)
            pathMeasure.getSegment(0f, end - pathLength, segmentPath, true)
        }
        canvas.drawPath(segmentPath, paint)
    }

    // ---------- Path / Effect ----------

    private fun updatePath() {
        updatePath(view.width.toFloat(), view.height.toFloat())
    }

    private fun updatePath(w: Float, h: Float) {
        if (w <= 0f || h <= 0f) {
            pathLength = 0f
            return
        }

        val inset = strokeWidth / 2f
        val rect = RectF(inset, inset, w - inset, h - inset)
        val maxRadius = minOf(rect.width(), rect.height()) / 2f
        val radius = cornerRadius.coerceIn(0f, maxRadius)

        fullPath.reset()
        fullPath.addRoundRect(rect, radius, radius, Path.Direction.CW)
        pathMeasure.setPath(fullPath, true)
        pathLength = pathMeasure.length
    }

    private fun updateDashEffect() {
        paint.pathEffect = if (dashWidth > 0f && dashGap > 0f) {
            DashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
        } else {
            null
        }
    }

    // ---------- Animation loop ----------

    private fun needsAnimating(s: InternalState): Boolean = when (s) {
        InternalState.IDLE, InternalState.HIDDEN -> false
        InternalState.LOADING, InternalState.SHRINKING, InternalState.GROWING -> true
    }

    private fun startAnimating() {
        if (animator != null) return
        lastFrameMs = SystemClock.elapsedRealtime()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { onFrame() }
            start()
        }
    }

    private fun stopAnimating() {
        animator?.cancel()
        animator = null
    }

    private fun onFrame() {
        val now = SystemClock.elapsedRealtime()
        val dt = (now - lastFrameMs).coerceAtLeast(0L)
        lastFrameMs = now

        val speed = 1f / loadingDuration
        val delta = speed * dt

        when (internalState) {
            InternalState.IDLE, InternalState.HIDDEN -> {
                stopAnimating()
                return
            }

            InternalState.LOADING -> {
                tailPos = wrap(tailPos + delta)
            }

            InternalState.SHRINKING -> {
                tailPos = wrap(tailPos + delta)
                segLen -= delta
                if (segLen <= targetSegLen) {
                    segLen = targetSegLen
                    internalState = settledInternalState
                    if (!needsAnimating(internalState)) stopAnimating()
                }
            }

            InternalState.GROWING -> {
                segLen += delta
                if (segLen >= targetSegLen) {
                    segLen = targetSegLen
                    internalState = settledInternalState
                    if (!needsAnimating(internalState)) stopAnimating()
                }
            }
        }
        view.invalidate()
    }

    private fun wrap(v: Float): Float {
        var x = v % 1f
        if (x < 0f) x += 1f
        return x
    }

    companion object {
        private const val EPS = 1e-4f
    }
}
