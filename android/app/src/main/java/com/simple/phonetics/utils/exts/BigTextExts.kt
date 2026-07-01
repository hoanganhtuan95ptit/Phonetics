package com.simple.phonetics.utils.exts

import com.simple.coreapp.utils.ext.Bold
import com.simple.coreapp.utils.ext.ForegroundColor
import com.simple.coreapp.utils.ext.RelativeSize
import com.simple.coreapp.utils.ext.RichRange
import com.simple.coreapp.utils.ext.RichStyle
import com.simple.coreapp.utils.ext.RichText
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.span.BigBold
import com.simple.ui.precompute.text.span.BigForegroundColor
import com.simple.ui.precompute.text.span.BigRelativeSize

fun BigText.toRich(): RichText {
    val richStyles = spans.map { bigStyle ->
        RichStyle(
            range = RichRange(bigStyle.range.start, bigStyle.range.end),
            styles = bigStyle.styles.mapNotNull { bigSpan ->
                when (bigSpan) {
                    is BigBold -> Bold
                    is BigForegroundColor -> ForegroundColor(bigSpan.color)
                    is BigRelativeSize -> RelativeSize(bigSpan.proportion)
                    else -> null
                }
            }
        )
    }
    return RichText(text, ArrayList(richStyles))
}
