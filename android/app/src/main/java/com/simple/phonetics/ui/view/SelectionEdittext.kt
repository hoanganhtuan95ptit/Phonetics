package com.simple.phonetics.ui.view

import android.content.Context
import android.util.AttributeSet

class SelectionEdittext @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : androidx.appcompat.widget.AppCompatEditText(context, attrs) {

    var onSelectionChangedListener: OnSelectionChangedListener? = null

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)

        onSelectionChangedListener?.onSelectionChanged(selStart, selEnd)
    }

    interface OnSelectionChangedListener {

        fun onSelectionChanged(selStart: Int, selEnd: Int)
    }
}