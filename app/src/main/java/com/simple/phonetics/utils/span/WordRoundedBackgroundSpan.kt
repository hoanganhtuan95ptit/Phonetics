//package com.simple.phonetics.utils.span
//
//import android.graphics.Canvas
//import android.graphics.Paint
//import android.text.style.LineHeightSpan
//import android.text.style.ReplacementSpan
//import android.util.Log
//import com.simple.coreapp.utils.extentions.toPx
//import kotlin.math.roundToInt
//
//class WordRoundedBackgroundSpan() : ReplacementSpan(), LineHeightSpan {
//
//    private val lineHeight = 56.toPx().toDouble()
//
//    override fun draw(canvas: Canvas, textxt: CharSequence, start: Int, end: Int, x: Float, top: Int, baseline: Int, bottom: Int, paint: Paint) {
//
////        val textSub = text.substring(start, end)
////
////        val textSize = paint.textSize
//
//
////        val textWidthMeasureOld = paint.measureText(textSub)
//
////        paint.style = Paint.Style.FILL
////        paint.textSize = textSize - 10
//
////        val textWidthMeasureNew = paint.measureText(textSub)
//
//
////        val spaceHorizontal = if (textWidthMeasureNew + spaceHorizontalMax + spaceHorizontalMax < widthMin) {
////
////            (widthMin - textWidthMeasureNew) / 2
////        } else {
////
////            min(textWidthMeasureOld - textWidthMeasureNew / 2, spaceHorizontalMax)
////        }
//
//
//        Log.d("tuanha1", "draw: ${paint.fontMetricsInt.toString()}")
//        canvas.drawText(text, start, end, x, baseline.toFloat(), paint)
//
////
////        val fontMetrics = paint.fontMetrics
////
////        val spaceVertical = baseline - top - fontMetrics.ascent.absoluteValue
////
////        val rectBottom = min(bottom.toFloat(), (baseline + fontMetrics.descent) + spaceVertical)
////
////        val rect = RectF(x, top.toFloat(), x + spaceHorizontal + textWidthMeasureNew + spaceHorizontal, rectBottom)
//
////        paint.color = color ?: paint.color
////        paint.style = Paint.Style.STROKE
////        paint.strokeWidth = 3f
////
////        paint.textSize = textSize
//    }
//
//    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
//
//
//        val textSub = text.substring(start, end)
//
//        val textWidthMeasureNew = paint.measureText(textSub)
//
//        return (textWidthMeasureNew).roundToInt()
//    }
//
//    override fun chooseHeight(text: CharSequence, start: Int, end: Int, spanstartv: Int, v: Int, fontMetricsInt: Paint.FontMetricsInt) {
//
//
//        Log.d("tuanha1", "chooseHeight: $fontMetricsInt")
////        if ((text as Spanned).getSpanEnd(this) in end - 1..end) {
////            fontMetricsInt.ascent -= 50
////        fontMetricsInt.leading += 50
//        fontMetricsInt.top  -= lineHeight.toInt()
////        fontMetricsInt.bottom += lineHeight.toInt()
////        fontMetricsInt.ascent = fontMetricsInt.top + 50
////        fontMetricsInt.leading = fontMetricsInt.bottom - 50
////        fontMetricsInt.descent = fontMetricsInt.bottom - 20
////        }
////
////        // In StaticLayout, line height is computed with descent - ascent
////        val currentHeight = fontMetricsInt.lineHeight()
//////
//////        // If current height is not positive, do nothing.
////        if (currentHeight <= 0) {
////            return
////        }
//////
////        val ceiledLineHeight = ceil(lineHeight).toInt()
////
////        val ratio = ceiledLineHeight * 1.0f / currentHeight
////
////        fontMetricsInt.descent = ceil(fontMetricsInt.descent * ratio.toDouble()).toInt()
////        fontMetricsInt.ascent = fontMetricsInt.descent - ceiledLineHeight
//
//
//        Log.d("tuanha1", "chooseHeight1: $fontMetricsInt")
//    }
//
//    fun Paint.FontMetricsInt.lineHeight(): Int = this.descent - this.ascent
//}