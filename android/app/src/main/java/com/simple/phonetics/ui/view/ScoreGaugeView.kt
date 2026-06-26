package com.simple.phonetics.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.simple.phonetics.R
import com.simple.phonetics.utils.exts.toPronunciationColor
import kotlin.math.min
import androidx.core.graphics.toColorInt

/**
 * Circular gauge view showing a score (0–100) with label and grade.
 *
 * The arc spans 270° with a gap at the bottom (startAngle = 135°).
 * - Track color: light gray
 * - Progress color: follows the score tier (green / orange / red)
 *
 * Usage in XML:
 *   <com.simple.phonetics.ui.view.ScoreGaugeView
 *       android:layout_width="160dp"
 *       android:layout_height="160dp"
 *       app:sg_progress="72"
 *       app:sg_grade="GRADE B"
 *       app:sg_label="ĐIỂM" />
 */
class ScoreGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val START_ANGLE = -90f
        private const val SWEEP_TOTAL = 360f

        private const val COLOR_TRACK = 0xFFE6E5DE.toInt()
        private const val COLOR_GREEN = 0xFF1ED760.toInt()

        private const val DEFAULT_TRACK_WIDTH_DP = 10f
    }

    // ---------- Public properties ----------

    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    var grade: String = ""
        set(value) {
            field = value
            invalidate()
        }

    var label: String = "ĐIỂM"
        set(value) {
            field = value
            invalidate()
        }

    // ---------- Paints ----------

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = COLOR_TRACK
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        color = "#111111".toColorInt()
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = "#6F6E69".toColorInt()
    }

    private val gradePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = COLOR_GREEN
        isFakeBoldText = true
    }

    private val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = "#111111".toColorInt()
    }

    private val arcRect = RectF()
    private val density = context.resources.displayMetrics.density
    private val trackWidthPx = DEFAULT_TRACK_WIDTH_DP * density

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.ScoreGaugeView)
            try {
                progress = a.getInt(R.styleable.ScoreGaugeView_sg_progress, progress)
                grade = a.getString(R.styleable.ScoreGaugeView_sg_grade) ?: grade
                label = a.getString(R.styleable.ScoreGaugeView_sg_label) ?: label
            } finally {
                a.recycle()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val inset = trackWidthPx / 2f + paddingLeft
        arcRect.set(inset, inset, w - inset - paddingRight, h - inset - paddingBottom)

        val size = min(w, h).toFloat()
        // Score number: ~30% of size
        scorePaint.textSize = size * 0.28f
        // "%" superscript size
        percentPaint.textSize = size * 0.14f
        // Label above score
        labelPaint.textSize = size * 0.10f
        // Grade below score
        gradePaint.textSize = size * 0.10f

        trackPaint.strokeWidth = trackWidthPx
        progressPaint.strokeWidth = trackWidthPx
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Track arc (full 270°)
        canvas.drawArc(arcRect, START_ANGLE, SWEEP_TOTAL, false, trackPaint)

        // 2. Progress arc
        if (progress > 0) {
            val sweep = SWEEP_TOTAL * progress / 100f
            progressPaint.color = progressColor()
            canvas.drawArc(arcRect, START_ANGLE, sweep, false, progressPaint)
        }

        // 3. Text in center
        val cx = arcRect.centerX()
        val cy = arcRect.centerY()

        // "ĐIỂM" label slightly above center
        val labelY = cy - scorePaint.textSize * 0.70f
        canvas.drawText(label, cx, labelY, labelPaint)

        // Score number
        val scoreStr = "$progress"
        val scoreY = cy + scorePaint.textSize * 0.35f
        canvas.drawText(scoreStr, cx - percentPaint.textSize * 0.5f, scoreY, scorePaint)

        // "%" right of score
        val percentX = cx + scorePaint.measureText(scoreStr) * 0.5f - percentPaint.textSize * 0.1f
        canvas.drawText("%", percentX, scoreY - scorePaint.textSize * 0.35f + percentPaint.textSize * 0.35f, percentPaint)

        // Grade below score
        if (grade.isNotEmpty()) {
            val gradeY = scoreY + gradePaint.textSize * 1.8f
            canvas.drawText(grade, cx, gradeY, gradePaint)
        }
    }

    private fun progressColor(): Int = progress.toPronunciationColor()
}
