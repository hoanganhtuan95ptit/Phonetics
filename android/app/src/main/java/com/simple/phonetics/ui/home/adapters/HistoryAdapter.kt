package com.simple.phonetics.ui.home.adapters

import android.view.View
import android.view.ViewGroup
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.base.BaseBindingViewHolder
import com.simple.event.sendEvent
import com.simple.phonetics.EventName
import com.simple.phonetics.R
import com.simple.phonetics.databinding.ItemPrecomputeBinding
import com.simple.phonetics.ui.common.adapters.PrecomputeAdapter
import com.simple.phonetics.ui.common.adapters.PrecomputeViewItem
import com.simple.phonetics.utils.TextViewMetrics
import com.simple.phonetics.utils.exts.dp
import com.simple.phonetics.utils.exts.sp
import com.simple.ui.precompute.image.BigImage
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.ImageNode
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.SpaceNode
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.node.linearChild
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.emptyText

@ItemAdapter
class HistoryAdapter(private val onItemClickV2: ((View, HistoryViewItem) -> Unit)? = null) : PrecomputeAdapter<HistoryViewItem>() {

    override val viewItemClass: Class<HistoryViewItem> by lazy {
        HistoryViewItem::class.java
    }

    override fun createViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<ItemPrecomputeBinding> {

        val viewHolder = super.createViewHolder(parent, viewType)!!

        viewHolder.binding.root.setOnClickListener { view ->

            val viewItem = getViewItem(viewHolder.bindingAdapterPosition) ?: return@setOnClickListener

            onItemClickV2?.invoke(view, viewItem)
            sendEvent(EventName.HISTORY_VIEW_ITEM_CLICKED, view to viewItem)
        }

        return viewHolder
    }
}

data class HistoryViewItem(
    override val id: String,
    override val maxWidth: Int,

    val text: BigText = emptyText(),
) : PrecomputeViewItem() {

    override val node: LayoutNode = LinearNode(
        orientation = Orientation.HORIZONTAL,
        padding = EdgeInsets.symmetric(
            h = 4.dp().toInt(),
            v = 8.dp().toInt()
        ),
        layoutWidth = LayoutDimension.MatchParent,
        children = listOf(
            ImageNode(
                source = BigImage(R.drawable.ic_history_24dp),
                layoutWidth = LayoutDimension.Fixed(20.dp().toInt()),
                layoutHeight = LayoutDimension.Fixed(20.dp().toInt())
            ).linearChild(),
            SpaceNode.horizontal(16.dp().toInt()).linearChild(),
            TextNode(
                text = text,
                layoutWidth = LayoutDimension.MatchParent,
                maxLines = 3
            ).linearChild()
        )
    )

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        text to "text",
    )
}