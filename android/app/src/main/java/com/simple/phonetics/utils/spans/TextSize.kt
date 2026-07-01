package com.simple.phonetics.utils.spans

import android.text.style.AbsoluteSizeSpan
import android.text.style.CharacterStyle
import androidx.annotation.Keep
import com.google.auto.service.AutoService
import com.simple.ui.precompute.text.BigImageSpan
import com.simple.ui.precompute.text.BigImageSpanConvert

data class BigTextSize(val sizeDip: Int) : BigImageSpan

@Keep
@AutoService(BigImageSpanConvert::class)
class BigTextSizeConvert : BigImageSpanConvert {
    override fun convert(bigSpan: BigImageSpan): CharacterStyle? {
        return if (bigSpan is BigTextSize) AbsoluteSizeSpan(bigSpan.sizeDip, true) else null
    }
}
