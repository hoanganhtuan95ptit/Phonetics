package com.simple.phonetics.utils.spans

import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.MetricAffectingSpan
import androidx.annotation.Keep
import com.google.auto.service.AutoService
import com.simple.coreapp.utils.ext.RichSpan
import com.simple.coreapp.utils.ext.RichSpanConvert

/**
 * Span áp dụng custom Typeface (font) cho đoạn text.
 * Dùng để kiểm soát font của title toolbar, header... từ ViewModel.
 * Ví dụ: CustomFont(Typeface.create("sans-serif-medium", Typeface.NORMAL))
 */
data class CustomFont(val typeface: Typeface) : RichSpan()

@Keep
@AutoService(RichSpanConvert::class)
class CustomFontConvert : RichSpanConvert {
    override fun getAndroidSpan(richSpan: RichSpan): CharacterStyle? {
        return (richSpan as? CustomFont)?.let { CustomFontAndroidSpan(it.typeface) }
    }
}

private class CustomFontAndroidSpan(private val typeface: Typeface) : MetricAffectingSpan() {
    override fun updateMeasureState(paint: TextPaint) {
        paint.typeface = typeface
    }

    override fun updateDrawState(tp: TextPaint) {
        tp.typeface = typeface
    }
}
