package com.simple.phonetics.ui.common.adapters

import android.view.View
import android.view.ViewGroup
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.base.BaseBindingViewHolder
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.event.sendEvent
import com.simple.ipa.entities.Ipa
import com.simple.phonetics.EventName
import com.simple.phonetics.databinding.ItemPrecomputeBinding
import com.simple.phonetics.utils.exts.dp
import com.simple.ui.precompute.node.BackgroundNode
import com.simple.ui.precompute.node.ConstraintChild
import com.simple.ui.precompute.node.ConstraintNode
import com.simple.ui.precompute.node.CrossAlign
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.node.linearChild
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.emptyText

@ItemAdapter
class IpaAdapters(private val onItemClickV2: ((View, IpaViewItem) -> Unit)? = null) : PrecomputeAdapter<IpaViewItem>() {

    override val viewItemClass: Class<IpaViewItem> by lazy {
        IpaViewItem::class.java
    }

    override fun createViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<ItemPrecomputeBinding> {

        val viewHolder = super.createViewHolder(parent, viewType)!!

        viewHolder.binding.root.setDebouncedClickListener { view ->

            val viewItem = getViewItem(viewHolder.bindingAdapterPosition) ?: return@setDebouncedClickListener

            onItemClickV2?.invoke(view, viewItem)
            sendEvent(EventName.IPA_VIEW_ITEM_CLICKED, view to viewItem)
        }

        return viewHolder
    }
}

data class IpaViewItem(
    override val id: String,
    override val maxWidth: Int,

    val data: Ipa,

    val ipa: BigText = emptyText(),
    val text: BigText = emptyText(),

    val background: Background = DEFAULT_BACKGROUND
) : PrecomputeViewItem() {

    override val node: LayoutNode = ConstraintNode(
        layoutWidth = LayoutDimension.Fixed(maxWidth),
        padding = EdgeInsets.all(4.dp().toInt()),
        children = listOf(
            ConstraintChild(
                id = "bg",
                node = BackgroundNode(
                    backgroundColor = background.backgroundColor,
                    strokeColor = background.strokeColor,
                    strokeWidth = background.strokeWidth.toFloat(),
                    cornerRadius = background.cornerRadius_TL.toFloat(),
                    layoutWidth = LayoutDimension.MatchParent,
                    layoutHeight = LayoutDimension.MatchParent,
                ),
                startToStartOf = "content",
                endToEndOf = "content",
                topToTopOf = "content",
                bottomToBottomOf = "content",
                width = LayoutDimension.MatchParent,
                height = LayoutDimension.MatchParent
            ),
            ConstraintChild(
                id = "content",
                node = ConstraintNode(
                    layoutWidth = LayoutDimension.MatchParent,
                    layoutHeight = LayoutDimension.Fixed(90.dp().toInt()),
                    children = listOf(
                        ConstraintChild(
                            id = "content_1",
                            node = LinearNode(
                                orientation = Orientation.VERTICAL,
                                crossAlign = CrossAlign.CENTER,
                                gap = 2.dp().toInt(),
                                children = listOf(
                                    TextNode(text = ipa).linearChild(),
                                    TextNode(text = text).linearChild()
                                )
                            ),
                            startToStartOf = ConstraintNode.PARENT,
                            endToEndOf = ConstraintNode.PARENT,
                            topToTopOf = ConstraintNode.PARENT,
                            bottomToBottomOf = ConstraintNode.PARENT
                        )
                    )
                ),
                startToStartOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
            )
        )
    )

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        ipa to "ipa",
        text to "text",
        background to "background"
    )
}
