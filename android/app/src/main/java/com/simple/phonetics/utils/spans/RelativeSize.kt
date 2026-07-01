package com.simple.phonetics.utils.spans

import android.text.style.CharacterStyle
import android.text.style.RelativeSizeSpan
import androidx.annotation.Keep
import com.google.auto.service.AutoService
import com.simple.ui.precompute.text.BigImageSpan
import com.simple.ui.precompute.text.BigImageSpanConvert

data class BigRelativeSize(val proportion: Float) : BigImageSpan

@Keep
@AutoService(BigImageSpanConvert::class)
class BigRelativeSizeConvert : BigImageSpanConvert {

    override fun convert(bigSpan: BigImageSpan): CharacterStyle? {
        return if (bigSpan is BigRelativeSize) RelativeSizeSpan(bigSpan.proportion) else null
    }
}
