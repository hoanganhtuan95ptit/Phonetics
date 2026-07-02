package com.simple.phonetics.ui.home.adapters

import android.view.ViewGroup
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.base.BaseBindingViewHolder
import com.simple.coreapp.utils.ext.updateMargin
import com.simple.event.sendEvent
import com.simple.phonetics.EventName
import com.simple.phonetics.databinding.ItemPrecomputeBinding
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.common.adapters.PrecomputeAdapter
import com.simple.phonetics.ui.common.adapters.PrecomputeViewItem
import com.simple.phonetics.utils.exts.dp
import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.LayoutEngine
import com.simple.ui.precompute.node.Constraints
import com.simple.ui.precompute.node.CrossAlign
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LineNode
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.SpaceNode
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.emptyText

@ItemAdapter
class SentenceAdapter : PrecomputeAdapter<SentenceViewItem>() {

    override val viewItemClass: Class<SentenceViewItem> by lazy {
        SentenceViewItem::class.java
    }

    override fun createViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<ItemPrecomputeBinding>? {

        val viewHolder = super.createViewHolder(parent, viewType) ?: return null

        viewHolder.binding.root.updateMargin(left = 4.dp().toInt(), right = 4.dp().toInt())
        viewHolder.binding.root.setOnClickListener { view ->

            val viewItem = getViewItem(viewHolder.bindingAdapterPosition) ?: return@setOnClickListener

            sendEvent(EventName.SENTENCE_VIEW_ITEM_CLICKED, view to viewItem)
        }

        return viewHolder
    }
}

data class SentenceViewItem(
    override val id: String,
    override val maxWidth: Int,

    val data: Sentence,

    var text: BigText = emptyText(),

    var isLast: Boolean = false
) : PrecomputeViewItem() {

    override val drawSpec: DrawSpec = LinearNode(
        orientation = Orientation.VERTICAL,
        crossAlign = CrossAlign.CENTER,
        gap = 8.dp().toInt(),
        padding = EdgeInsets(top = 14.dp().toInt()),
        layoutWidth = LayoutDimension.MatchParent,
        children = listOfNotNull(
            LineNode(
                color = 0xFFB8B8B8.toInt(),
                strokeWidth = 1.5f * 1.dp(),
                dashWidth = 4.dp(),
                dashGap = 4.dp(),
                layoutWidth = LayoutDimension.Fixed(240.dp().toInt()),
                layoutHeight = LayoutDimension.Fixed(2.dp().toInt()),
            ),
            TextNode(
                text = text,
                layoutWidth = LayoutDimension.MatchParent,
            ),
            SpaceNode(
                layoutHeight = LayoutDimension.Fixed(8.dp().toInt())
            ),
            LineNode(
                color = 0xFFB8B8B8.toInt(),
                strokeWidth = 1.dp(),
                layoutWidth = LayoutDimension.MatchParent,
                layoutHeight = LayoutDimension.WrapContent,
            ).takeIf {
                !isLast
            },
            SpaceNode(
                layoutHeight = LayoutDimension.Fixed(8.dp().toInt())
            )
        )
    ).let {

        LayoutEngine.measure(node = it, constraints = Constraints(maxWidth), id = id)
    }

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        text to "text",
        isLast to "isLast"
    )
}
