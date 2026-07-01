package com.simple.phonetics.utils.spans

import android.text.style.AbsoluteSizeSpan
import android.text.style.CharacterStyle
import androidx.annotation.Keep
import com.google.auto.service.AutoService
import com.simple.ui.precompute.text.BigSpan
import com.simple.ui.precompute.text.BigSpanConvert

data class BigTextSize(val sizeDip: Int) : BigSpan()

@Keep
@AutoService(BigSpanConvert::class)
class BigTextSizeConvert : BigSpanConvert {
    override fun getAndroidSpan(bigSpan: BigSpan): CharacterStyle? {
        return if (bigSpan is BigTextSize) AbsoluteSizeSpan(bigSpan.sizeDip, true) else null
    }
}
