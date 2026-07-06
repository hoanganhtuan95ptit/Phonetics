package com.simple.phonetics.ui.base.adapters

import android.view.ViewGroup
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.base.BaseBindingViewHolder
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
import com.simple.coreapp.ui.view.DEFAULT_MARGIN
import com.simple.coreapp.ui.view.DEFAULT_PADDING
import com.simple.coreapp.ui.view.DEFAULT_SIZE
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.event.sendEvent
import com.simple.phonetics.EventName
import com.simple.phonetics.R
import com.simple.phonetics.databinding.ItemPrecomputeBinding
import com.simple.phonetics.ui.common.adapters.PrecomputeAdapter
import com.simple.phonetics.ui.common.adapters.PrecomputeViewItem
import com.simple.ui.precompute.node.ConstraintChild
import com.simple.ui.precompute.node.ConstraintNode
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.emptyText

@ItemAdapter
class TextSimpleAdapter : PrecomputeAdapter<TextSimpleViewItem>() {

    override val viewItemClass: Class<TextSimpleViewItem> by lazy {
        TextSimpleViewItem::class.java
    }

    override fun createViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<ItemPrecomputeBinding> {

        val viewHolder = super.createViewHolder(parent, viewType)!!

        viewHolder.binding.root.setDebouncedClickListener { view ->

            val viewItem = getViewItem(viewHolder.bindingAdapterPosition) ?: return@setDebouncedClickListener

            sendEvent(EventName.TEXT_SIMPLE_VIEW_ITEM_CLICKED, view to viewItem)
        }

        return viewHolder
    }
}

data class TextSimpleViewItem(
    override val id: String,
    override val maxWidth: Int,
    val data: Any? = null,

    var text: BigText = emptyText(),
    var textStyle: Int = R.style.TextBody1,

    val textSize: Size = DEFAULT_SIZE,
    val textMargin: Margin = DEFAULT_MARGIN,
    val textPadding: Padding = DEFAULT_PADDING,
    val textBackground: Background = DEFAULT_BACKGROUND,

    var size: Size = DEFAULT_SIZE,
    val margin: Margin = DEFAULT_MARGIN,
    val padding: Padding = DEFAULT_PADDING,
    val background: Background = DEFAULT_BACKGROUND
) : PrecomputeViewItem() {


    override val node: LayoutNode by lazy {
        ConstraintNode(
            children = listOf(
                ConstraintChild(
                    id = "text",
                    node = TextNode(
                        text = text,
                        padding = EdgeInsets(
                            left = textPadding.left,
                            top = textPadding.top,
                            right = textPadding.right,
                            bottom = textPadding.bottom
                        ),
                    ),
                    startToStartOf = ConstraintNode.PARENT,
                    topToTopOf = ConstraintNode.PARENT,
                )
            )
        )
    }

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        text to "text",
        textStyle to "textStyle",

        textSize to "textSize",
        textMargin to "textMargin",
        textPadding to "textPadding",
        textBackground to "textBackground",

        size to "size",
        margin to "margin",
        padding to "padding",
        background to "background"
    )
}
