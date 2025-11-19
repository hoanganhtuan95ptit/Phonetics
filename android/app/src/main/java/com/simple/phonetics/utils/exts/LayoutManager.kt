package com.simple.phonetics.utils.exts

import android.content.Context
import com.google.android.flexbox.FlexboxLayoutManagerWrap
import com.google.android.flexbox.FlexboxLayoutManagerWrap.OnErrorListener

fun createFlexboxLayoutManager(context: Context?, error: ((Exception) -> Unit)? = null): FlexboxLayoutManagerWrap {

    val layoutManager = NoAutoScrollFlexboxLayoutManager(context)

    layoutManager.onErrorListener = OnErrorListener {

        error?.invoke(it)
    }

    return layoutManager
}

class NoAutoScrollFlexboxLayoutManager(context: Context?) : FlexboxLayoutManagerWrap(context) {

    var isBinding = false

    override fun canScrollVertically(): Boolean {
        return !isBinding && super.canScrollVertically()
    }
}