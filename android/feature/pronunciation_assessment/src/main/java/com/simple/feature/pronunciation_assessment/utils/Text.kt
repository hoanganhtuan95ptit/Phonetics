package com.simple.feature.pronunciation_assessment.utils

import com.simple.ui.precompute.text.BigRange
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.toBig

operator fun String.plus(other: BigText): BigText {

    return this.toBig() + other
}

operator fun BigText.plus(text: String): BigText {

    return this + text.toBig()
}

operator fun BigText.plus(other: BigText): BigText {
    val offset = this.text.length

    val shiftedSpans = other.spans.map { richStyle ->
        richStyle.copy(
            range = BigRange(
                start = richStyle.range.start + offset,
                end = richStyle.range.end + offset
            )
        )
    }

    return BigText(
        text = this.text + other.text,
        spans = ArrayList(this.spans + shiftedSpans)
    )
}
