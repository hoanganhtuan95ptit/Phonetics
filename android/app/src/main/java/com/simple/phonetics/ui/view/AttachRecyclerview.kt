package com.simple.phonetics.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.simple.adapter.MultiAdapter

class AttachRecyclerview @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {

    var onChildViewListener: OnChildViewListener? = null

    init {

        adapter = MultiAdapter()
    }

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        onChildViewListener?.onViewAdded(child)
    }

    override fun onViewRemoved(child: View?) {
        super.onViewRemoved(child)
        onChildViewListener?.onViewRemoved(child)
    }

    interface OnChildViewListener {
        fun onViewAdded(child: View?)
        fun onViewRemoved(child: View?)
    }
}