package com.simple.feature.pronunciation_assessment.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin

/**
 * View hiển thị hiệu ứng sóng âm khi đang ghi âm.
 *
 * Gồm [barCount] thanh dọc, mỗi thanh dao động lên-xuống độc lập theo dạng sin với
 * pha lệch đều nhau, tạo cảm giác sóng chạy qua. Hỗ trợ cập nhật biên độ thực tế
 * từ microphone qua [setAmplitude].
 *
 * Trạng thái:
 *  - [startRecording] : bắt đầu animate.
 *  - [stopRecording]  : animate thu nhỏ về idle rồi dừng.
 *  - [resetIdle]      : snap ngay về trạng thái idle (không animate).
 */
class RecordingWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ---------- Giao diện ----------

    /** Màu các thanh sóng. */
    var barColor: Int = Color.parseColor("#4CAF50")
        set(value) {
            field = value
            paint.color = value
            invalidate()
        }

    /** Số thanh sóng. */
    var barCount: Int = 15
        set(value) {
            field = value.coerceIn(1, 32)
            invalidate()
        }

    /** Tỉ lệ khoảng cách giữa các thanh so với chiều rộng thanh (0 = không có khoảng cách). */
    var barGapRatio: Float = 0.4f
        set(value) {
            field = value.coerceIn(0f, 2f)
            invalidate()
        }

    /** Chiều cao tối thiểu mỗi thanh theo tỉ lệ so với height view (0..1). */
    var minHeightRatio: Float = 0.22f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    /** Chiều cao tối đa mỗi thanh theo tỉ lệ so với height view (0..1). */
    var maxHeightRatio: Float = 0.85f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    /** Bo góc của mỗi thanh (px). -1 = tự động = nửa chiều rộng thanh (pill shape). */
    var barCornerRadius: Float = -1f
        set(value) {
            field = value
            invalidate()
        }

    /** Tốc độ animation: thời gian (ms) để sóng đi hết 1 chu kỳ. */
    var cycleDurationMs: Long = 800L
        set(value) {
            field = value.coerceAtLeast(100L)
        }

    // ---------- Nội bộ ----------

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = barColor
    }

    private val barRect = RectF()

    /** Phase hiện tại của animation [0, 2π). */
    private var phase: Float = 0f

    /** Biên độ hiện tại (0..1), cập nhật từ microphone. */
    private var amplitude: Float = 1f

    /** Biên độ "target" để tween mượt khi đổi giá trị. */
    private var targetAmplitude: Float = 1f

    /** true = đang ở chế độ ghi âm (animate đầy đủ). */
    private var isRecording: Boolean = true

    /** Animator chạy vòng vô tận để update phase và invalidate. */
    private var animator: ValueAnimator? = null

    private var lastFrameMs: Long = 0L

    // ---------- API ----------

    /** Bắt đầu hiệu ứng ghi âm. */
    fun startRecording() {
        isRecording = true
        targetAmplitude = 1f
        startAnimating()
    }

    /** Dừng hiệu ứng (animate thu nhỏ dần về idle). */
    fun stopRecording() {
        isRecording = false
        targetAmplitude = 0f
        // Giữ animator chạy để tween amplitude về 0 rồi dừng
    }

    /** Snap ngay về trạng thái idle, không animate. */
    fun resetIdle() {
        isRecording = false
        amplitude = 0f
        targetAmplitude = 0f
        stopAnimating()
        invalidate()
    }

    /**
     * Cập nhật biên độ từ microphone.
     * @param value Giá trị chuẩn hóa trong khoảng [0f, 1f].
     */
    fun setAmplitude(value: Float) {
        targetAmplitude = value.coerceIn(0f, 1f)
    }

    // ---------- Lifecycle ----------

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isRecording) startAnimating()
    }

    override fun onDetachedFromWindow() {
        stopAnimating()
        super.onDetachedFromWindow()
    }

    // ---------- Animation ----------

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

        // Tween amplitude về targetAmplitude với tốc độ ~3 đơn vị/giây
        val ampSpeed = 0.003f * dt
        amplitude = when {
            amplitude < targetAmplitude -> (amplitude + ampSpeed).coerceAtMost(targetAmplitude)
            amplitude > targetAmplitude -> (amplitude - ampSpeed).coerceAtLeast(targetAmplitude)
            else -> amplitude
        }

        // Cập nhật phase
        val cyclePerMs = TWO_PI / cycleDurationMs
        phase = (phase + cyclePerMs * dt) % TWO_PI

        // Nếu đã dừng và amplitude về 0, stop animator
        if (!isRecording && amplitude <= 0.001f) {
            amplitude = 0f
            stopAnimating()
        }

        invalidate()
    }

    // ---------- Draw ----------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f || barCount <= 0) return

        val totalGapRatio = barGapRatio * (barCount - 1)
        val barWidth = w / (barCount + totalGapRatio)
        val gap = barWidth * barGapRatio
        val minH = h * minHeightRatio
        val maxH = h * maxHeightRatio
        val cornerR = if (barCornerRadius < 0f) barWidth / 2f else barCornerRadius

        for (i in 0 until barCount) {
            // Mỗi thanh có pha lệch đều theo vị trí
            val phaseOffset = (TWO_PI / barCount) * i
            val sineValue = sin(phase + phaseOffset).toFloat() // -1..1

            // Trọng số vị trí: 0 ở hai đầu, 1 ở giữa → hai đầu không dao động
            val posWeight = if (barCount > 1) sin(Math.PI.toFloat() * i / (barCount - 1)) else 1f

            // Hai đầu = minH, trung tâm dao động đầy đủ quanh center
            val range = (maxH - minH) / 2f
            val center = (maxH + minH) / 2f
            val animatedH = center + amplitude * range * sineValue
            val barH = minH * (1f - posWeight) + animatedH * posWeight

            val left = i * (barWidth + gap)
            val right = left + barWidth
            val top = (h - barH) / 2f
            val bottom = top + barH

            barRect.set(left, top, right, bottom)
            canvas.drawRoundRect(barRect, cornerR, cornerR, paint)
        }
    }

    companion object {
        private const val TWO_PI = (2.0 * Math.PI).toFloat()
    }
}