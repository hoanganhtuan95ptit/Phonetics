package com.simple.phonetics.utils.exts

import android.view.Gravity
import android.view.ViewGroup
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
import com.simple.coreapp.ui.view.DEFAULT_MARGIN
import com.simple.coreapp.ui.view.DEFAULT_PADDING
import com.simple.coreapp.ui.view.DEFAULT_SIZE
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.DP
import com.simple.phonetics.ui.common.adapters.texts.ClickBigTextViewItem
import com.simple.phonetics.ui.common.adapters.texts.NoneBigTextViewItem
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.BigTextBuilder
import com.simple.ui.precompute.text.span.BigTextSize
import com.simple.ui.precompute.text.toBuilder
import com.simple.ui.precompute.text.with

fun TitleViewItem(
    id: String,
    text: BigText,

    textSize: Float? = 20f,
    textMargin: Margin = DEFAULT_MARGIN,

    size: Size = Size(
        width = ViewGroup.LayoutParams.MATCH_PARENT,
        height = ViewGroup.LayoutParams.WRAP_CONTENT
    )
) = NoneBigTextViewItem(
    id = id,
    text = text,
    textStyle = TextStyle(
        textSize = textSize
    ),
    textSize = DEFAULT_SIZE,
    textMargin = textMargin,
    textPadding = DEFAULT_PADDING,
    textBackground = DEFAULT_BACKGROUND,

    size = size,
    margin = Margin(
        marginHorizontal = DP.DP_4
    ),
    padding = DEFAULT_PADDING,
    background = DEFAULT_BACKGROUND
)

fun OptionViewItem(
    id: String,

    data: Any? = null,

    text: BigText,

    strokeColor: Int,
    backgroundColor: Int,
) = ClickBigTextViewItem(
    id = id,
    data = data,
    text = text,
    textStyle = TextStyle(
        textSize = 16f,
        textGravity = Gravity.START
    ),
    padding = Padding(
        top = DP.DP_6,
        right = DP.DP_8,
        bottom = DP.DP_6
    ),
    textPadding = Padding(
        top = DP.DP_8,
        bottom = DP.DP_8,
        left = DP.DP_16,
        right = DP.DP_16
    ),
    textBackground = Background(
        cornerRadius = DP.DP_100,
        strokeWidth = DP.DP_1,
        strokeColor = strokeColor,
        backgroundColor = backgroundColor
    )
)

fun OptionPrimaryViewItem(
    id: String,

    data: Any? = null,

    text: BigText,
    textSize: Size? = null,

    size: Size? = null,
    strokeColor: Int,
) = ClickBigTextViewItem(
    id = id,
    data = data,
    text = text,
    textSize = textSize ?: Size(
        width = ViewGroup.LayoutParams.MATCH_PARENT,
        height = ViewGroup.LayoutParams.MATCH_PARENT
    ),
    textStyle = TextStyle(
        textGravity = Gravity.CENTER
    ),
    textPadding = Padding(
        left = DP.DP_16,
        right = DP.DP_16
    ),
    textBackground = DEFAULT_BACKGROUND,

    size = size ?: Size(
        height = DP.DP_76,
        width = ViewGroup.LayoutParams.WRAP_CONTENT
    ),
    margin = Margin(
        marginVertical = DP.DP_4,
        marginHorizontal = DP.DP_4
    ),
    background = Background(
        strokeColor = strokeColor,
        strokeWidth = DP.DP_2,
        cornerRadius = DP.DP_16
    ),

    imageLeft = null,
    imageRight = null
)

fun ButtonViewItem(
    id: String,

    data: Any? = null,

    text: BigText,

    strokeColor: Int,
    backgroundColor: Int,
) = ClickBigTextViewItem(
    id = id,
    data = data,
    text = text,
    textStyle = TextStyle(
        textSize = 18f,
        textGravity = Gravity.CENTER
    ),
    textSize = Size(
        width = ViewGroup.LayoutParams.MATCH_PARENT,
        height = ViewGroup.LayoutParams.MATCH_PARENT
    ),
    textPadding = Padding(
        paddingVertical = DP.DP_16,
        paddingHorizontal = DP.DP_16,
    ),
    textBackground = Background(
        cornerRadius = DP.DP_100,
        strokeWidth = DP.DP_1,
        strokeColor = strokeColor,
        backgroundColor = backgroundColor
    ),
    size = Size(
        width = ViewGroup.LayoutParams.MATCH_PARENT,
        height = ViewGroup.LayoutParams.WRAP_CONTENT
    )
)



fun String.withStyleDisplayLarge(): BigTextBuilder {
    return toBuilder().with(BigTextSize(57.sp().toInt()))
}

fun String.withStyleDisplayMedium(): BigTextBuilder {
    return toBuilder().with(BigTextSize(45.sp().toInt()))
}

fun String.withStyleDisplaySmall(): BigTextBuilder {
    return toBuilder().with(BigTextSize(36.sp().toInt()))
}

fun String.withStyleHeadlineLarge(): BigTextBuilder {
    return toBuilder().with(BigTextSize(32.sp().toInt()))
}

fun String.withStyleHeadlineMedium(): BigTextBuilder {
    return toBuilder().with(BigTextSize(28.sp().toInt()))
}

fun String.withStyleHeadlineSmall(): BigTextBuilder {
    return toBuilder().with(BigTextSize(24.sp().toInt()))
}

fun String.withStyleTitleLarge(): BigTextBuilder {
    return toBuilder().with(BigTextSize(22.sp().toInt()))
}

fun String.withStyleTitleMedium(): BigTextBuilder {
    return toBuilder().with(BigTextSize(16.sp().toInt()))
}

fun String.withStyleTitleSmall(): BigTextBuilder {
    return toBuilder().with(BigTextSize(14.sp().toInt()))
}

fun String.withStyleBodyLarge(): BigTextBuilder {
    return toBuilder().with(BigTextSize(16.sp().toInt()))
}

fun String.withStyleBodyMedium(): BigTextBuilder {
    return toBuilder().with(BigTextSize(14.sp().toInt()))
}

fun String.withStyleBodySmall(): BigTextBuilder {
    return toBuilder().with(BigTextSize(12.sp().toInt()))
}

fun String.withStyleLabelLarge(): BigTextBuilder {
    return toBuilder().with(BigTextSize(14.sp().toInt()))
}

fun String.withStyleLabelMedium(): BigTextBuilder {
    return toBuilder().with(BigTextSize(12.sp().toInt()))
}

fun String.withStyleLabelSmall(): BigTextBuilder {
    return toBuilder().with(BigTextSize(11.sp().toInt()))
}
