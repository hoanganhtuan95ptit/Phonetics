package com.simple.phonetics.utils.exts

import android.view.ViewGroup
import com.simple.coreapp.ui.adapters.texts.NoneTextViewItem
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.ui.view.TextStyle

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
    textSize = Size(),
    textMargin = Margin(),
    textPadding = Padding(),
    textBackground = BACKGROUND_DEFAULT,

    size = Size(
        width = ViewGroup.LayoutParams.MATCH_PARENT,
        height = ViewGroup.LayoutParams.WRAP_CONTENT
    ),
    margin = Margin(),
    padding = Padding(),
    background = BACKGROUND_DEFAULT
)