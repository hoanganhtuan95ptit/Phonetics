package com.simple.feature.pronunciation_assessment.ui.views.draws

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import com.bumptech.glide.Glide
import com.simple.coreapp.utils.ext.RichText
import com.simple.image.GifImageData
import com.simple.image.RichImage
import com.simple.image.RichImageData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────
// Interface
// ─────────────────────────────────────────────────────────────

interface DrawPaint {
    val frame: Rect
    fun onDraw(canvas: Canvas)
    fun onAttach(view: View) {}
    fun onDetach() {}
}

// ─────────────────────────────────────────────────────────────
// RichImagePaint — tự load drawable & quản lý lifecycle
// ─────────────────────────────────────────────────────────────

class RichImagePaint(
    override val frame: Rect,
    val richImage: RichImage,
    private val context: Context,       // nên truyền applicationContext
) : DrawPaint {

    private var view: View? = null
    private var drawable: Drawable? = null
    private var loadJob: Job? = null

    private val callback = object : Drawable.Callback {
        override fun invalidateDrawable(who: Drawable) {
            view?.postInvalidateOnAnimation()
        }
        override fun scheduleDrawable(who: Drawable, what: Runnable, time: Long) {
            view?.postOnAnimationDelayed(what, (time - SystemClock.uptimeMillis()).coerceAtLeast(0L))
        }
        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
            view?.removeCallbacks(what)
        }
    }

    override fun onAttach(view: View) {
        this.view = view
        if (drawable != null) {
            (drawable as? Animatable)?.start()
            view.postInvalidateOnAnimation()
        } else {
            loadJob = CoroutineScope(Dispatchers.IO).launch {
                val loaded = loadDrawable() ?: return@launch
                withContext(Dispatchers.Main) { setDrawable(loaded) }
            }
        }
    }

    override fun onDetach() {
        loadJob?.cancel()
        loadJob = null
        (drawable as? Animatable)?.stop()
        drawable?.callback = null
        view = null
    }

    private fun setDrawable(drawable: Drawable?) {
        (this.drawable as? Animatable)?.stop()
        this.drawable?.callback = null
        this.drawable = drawable?.also {
            it.setBounds(frame.left, frame.top, frame.right, frame.bottom)
            it.callback = callback
            if (view != null) (it as? Animatable)?.start()
        }
        view?.postInvalidateOnAnimation()
    }

    private fun loadDrawable(): Drawable? {
        val source = (richImage as? RichImageData)?.data ?: return null
        val w = frame.width().coerceAtLeast(1)
        val h = frame.height().coerceAtLeast(1)
        return runCatching {
            if (richImage is GifImageData)
                Glide.with(context).asGif().load(source).submit(w, h).get()
            else
                Glide.with(context).asDrawable().load(source).submit(w, h).get()
        }.getOrNull()
    }

    override fun onDraw(canvas: Canvas) {
        drawable?.draw(canvas)
    }
}

// ─────────────────────────────────────────────────────────────
// RichTextPaint — TextPaint per-instance để hỗ trợ custom style
// ─────────────────────────────────────────────────────────────

class RichTextPaint(
    override val frame: Rect,
    val gravity: Int,
    val richText: RichText,
    textSize: Float = 12f,
    textColor: Int = Color.BLACK,
) : DrawPaint {

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = textSize
        this.color = textColor
    }

    override fun onDraw(canvas: Canvas) {
        val text = richText.text
        if (text.isEmpty() || frame.width() <= 0) return

        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, frame.width())
            .setAlignment(gravity.toLayoutAlignment())
            .setIncludePad(false)
            .setLineSpacing(0f, 1f)
            .build()

        val dx = when (gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
            Gravity.CENTER_HORIZONTAL -> frame.left + (frame.width() - layout.width) / 2f
            Gravity.RIGHT, Gravity.END -> frame.right - layout.width.toFloat()
            else -> frame.left.toFloat()
        }
        val dy = when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
            Gravity.CENTER_VERTICAL -> frame.top + (frame.height() - layout.height) / 2f
            Gravity.BOTTOM -> frame.bottom - layout.height.toFloat()
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

// ─────────────────────────────────────────────────────────────
// DrawView — chỉ delegate lifecycle, không biết gì về nội dung
// ─────────────────────────────────────────────────────────────

class DrawView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var drawPaints: List<DrawPaint> = emptyList()
        set(value) {
            if (isAttachedToWindow) field.forEach { it.onDetach() }
            field = value
            if (isAttachedToWindow) value.forEach { it.onAttach(this) }
            postInvalidateOnAnimation()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawPaints.forEach { it.onDraw(canvas) }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        drawPaints.forEach { it.onAttach(this) }
    }

    override fun onDetachedFromWindow() {
        drawPaints.forEach { it.onDetach() }
        super.onDetachedFromWindow()
    }
}