package com.simple.feature.pronunciation_assessment.utils

import com.simple.coreapp.utils.ext.RichRange
import com.simple.coreapp.utils.ext.RichText

operator fun RichText.plus(other: RichText): RichText {
    val offset = this.text.length

    val shiftedSpans = other.spans.map { richStyle ->
        richStyle.copy(
            range = RichRange(
                start = richStyle.range.start + offset,
                end = richStyle.range.end + offset
            )
        )
    }

    return RichText(
        text = this.text + other.text,
        spans = ArrayList(this.spans + shiftedSpans)
    )
}