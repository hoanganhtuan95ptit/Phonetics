package com.simple.phonetics.utils.exts

import android.content.Context
import com.google.android.flexbox.FlexboxLayoutManagerWrap
import com.google.android.flexbox.FlexboxLayoutManagerWrap.OnErrorListener

interface CanDisableScroll {

    var isCanScroll: Boolean
}

fun createFlexboxLayoutManager(context: Context?, error: ((Exception) -> Unit)? = null): FlexboxLayoutManagerWrap {

    val layoutManager = NoAutoScrollFlexboxLayoutManager(context)

    layoutManager.onErrorListener = OnErrorListener {

        error?.invoke(it)
    }

    return layoutManager
}

class NoAutoScrollFlexboxLayoutManager(context: Context?) : FlexboxLayoutManagerWrap(context), CanDisableScroll {

    override var isCanScroll: Boolean = true

    override fun canScrollVertically(): Boolean {
        return isCanScroll && super.canScrollVertically()
    }

    override fun canScrollHorizontally(): Boolean {
        return isCanScroll && super.canScrollHorizontally()
    }
}