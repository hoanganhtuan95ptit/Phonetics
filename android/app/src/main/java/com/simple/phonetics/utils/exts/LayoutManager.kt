package com.simple.phonetics.utils.exts

import android.content.Context
import com.google.android.flexbox.FlexboxLayoutManagerWrap
import com.google.android.flexbox.FlexboxLayoutManagerWrap.OnErrorListener

fun createFlexboxLayoutManager(context: Context?, error: ((Exception) -> Unit)? = null): FlexboxLayoutManagerWrap {

    val layoutManager = FlexboxLayoutManagerWrap(context)

    layoutManager.onErrorListener = OnErrorListener {

        error?.invoke(it)
    }

    return layoutManager
}