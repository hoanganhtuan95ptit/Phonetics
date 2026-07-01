package com.simple.phonetics.utils.spans

import android.text.style.CharacterStyle
import android.text.style.RelativeSizeSpan
import androidx.annotation.Keep
import com.google.auto.service.AutoService
import com.simple.ui.precompute.text.BigSpan
import com.simple.ui.precompute.text.BigSpanConvert

data class BigRelativeSize(val proportion: Float) : BigSpan()

@Keep
@AutoService(BigSpanConvert::class)
class BigRelativeSizeConvert : BigSpanConvert {

    override fun getAndroidSpan(bigSpan: BigSpan): CharacterStyle? {
        return if (bigSpan is BigRelativeSize) RelativeSizeSpan(bigSpan.proportion) else null
    }
}
