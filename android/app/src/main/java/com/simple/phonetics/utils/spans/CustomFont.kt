package com.simple.phonetics.utils.spans

import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.MetricAffectingSpan
import androidx.annotation.Keep
import com.google.auto.service.AutoService
import com.simple.ui.precompute.text.BigSpan
import com.simple.ui.precompute.text.BigSpanConvert

/**
 * Span áp dụng custom Typeface (font) cho đoạn text.
 * Dùng để kiểm soát font của title toolbar, header... từ ViewModel.
 * Ví dụ: CustomFont(Typeface.create("sans-serif-medium", Typeface.NORMAL))
 */
data class BigCustomFont(val typeface: Typeface) : BigSpan()

@Keep
@AutoService(BigSpanConvert::class)
class BigCustomFontConvert : BigSpanConvert {
    override fun getAndroidSpan(bigSpan: BigSpan): CharacterStyle? {
        return (bigSpan as? BigCustomFont)?.let { CustomFontAndroidSpan(it.typeface) }
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
