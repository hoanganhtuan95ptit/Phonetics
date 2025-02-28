package com.simple.phonetics.utils.exts

import android.view.ViewGroup
import com.simple.coreapp.ui.adapters.texts.NoneTextViewItem
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
import com.simple.coreapp.ui.view.DEFAULT_MARGIN
import com.simple.coreapp.ui.view.DEFAULT_PADDING
import com.simple.coreapp.ui.view.DEFAULT_SIZE
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle
import com.simple.coreapp.utils.ext.DP

fun TitleViewItem(
    id: String,
    text: CharSequence,
    textSize: Float = 20f
) = NoneTextViewItem(
    id = id,
    text = text,
    textStyle = TextStyle(
        textSize = textSize
    ),
    textSize = DEFAULT_SIZE,
    textMargin = DEFAULT_MARGIN,
    textPadding = DEFAULT_PADDING,
    textBackground = DEFAULT_BACKGROUND,

    size = Size(
        width = ViewGroup.LayoutParams.MATCH_PARENT,
        height = ViewGroup.LayoutParams.WRAP_CONTENT
    ),
    margin = Margin(
        marginHorizontal = DP.DP_4
    ),
    padding = DEFAULT_PADDING,
    background = DEFAULT_BACKGROUND
)