package com.simple.feature.pronunciation_assessment.utils

import com.simple.coreapp.utils.ext.RichRange
import com.simple.coreapp.utils.ext.RichText
import com.simple.coreapp.utils.ext.toRich

operator fun String.plus(other: RichText): RichText {

    return this.toRich() + other
}

operator fun RichText.plus(text: String): RichText {

    return this + text.toRich()
}

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