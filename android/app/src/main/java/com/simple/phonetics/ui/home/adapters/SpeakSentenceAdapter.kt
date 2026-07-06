package com.simple.phonetics.ui.home.adapters

import android.view.ViewGroup
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.base.BaseBindingViewHolder
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.updateMargin
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.R
import com.simple.phonetics.databinding.ItemPrecomputeBinding
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.common.adapters.PrecomputeAdapter
import com.simple.phonetics.ui.common.adapters.PrecomputeViewItem
import com.simple.phonetics.utils.exts.dp
import com.simple.ui.precompute.LayoutEngine
import com.simple.ui.precompute.LayoutResult
import com.simple.ui.precompute.image.ColorFilter
import com.simple.ui.precompute.image.addTransform
import com.simple.ui.precompute.image.build
import com.simple.ui.precompute.image.toBuilder
import com.simple.ui.precompute.node.BackgroundNode
import com.simple.ui.precompute.node.ConstraintChild
import com.simple.ui.precompute.node.ConstraintNode
import com.simple.ui.precompute.node.Constraints
import com.simple.ui.precompute.node.CrossAlign
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.ImageNode
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.node.linearChild
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.emptyText

@ItemAdapter
class SpeakSentenceAdapter : PrecomputeAdapter<SpeakSentenceViewItem>() {

    override fun createViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<ItemPrecomputeBinding>? {
        val viewHolder = super.createViewHolder(parent, viewType) ?: return null

        viewHolder.binding.root.updateMargin(left = 4.dp().toInt(), right = 4.dp().toInt())
        viewHolder.binding.root.setDebouncedClickListener {

            val item = getViewItem(viewHolder.bindingAdapterPosition) ?: return@setDebouncedClickListener

            sendDeeplink(deepLink = DeeplinkManager.SPEAK, extras = mapOf(Param.TEXT to item.sentence.text))
        }

        return viewHolder
    }

    override val viewItemClass: Class<SpeakSentenceViewItem> by lazy {
        SpeakSentenceViewItem::class.java
    }
}

data class SpeakSentenceViewItem(
    override val id: String,
    override val maxWidth: Int,

    val sentence: Sentence,

    val text: BigText = emptyText(),

    val contentColor: Int,
) : PrecomputeViewItem() {

    override val node: LayoutNode = ConstraintNode(
        children = listOf(
            ConstraintChild(
                id = "outline",
                node = BackgroundNode(
                    strokeColor = contentColor,
                    strokeWidth = 1.dp(),
                    cornerRadius = 16.dp(),
                    dashWidth = 4.dp(),
                    dashGap = 4.dp(),
                    layoutWidth = LayoutDimension.MatchParent,
                    layoutHeight = LayoutDimension.MatchParent,
                ),
                startToStartOf = "content",
                endToEndOf = "content",
                topToTopOf = "content",
                bottomToBottomOf = "content",
                width = LayoutDimension.MatchParent,
                height = LayoutDimension.MatchParent,
            ),
            ConstraintChild(
                id = "content",
                node = LinearNode(
                    orientation = Orientation.HORIZONTAL,
                    crossAlign = CrossAlign.CENTER,
                    gap = 8.dp().toInt(),
                    padding = EdgeInsets.symmetric(h = 8.dp().toInt(), v = 8.dp().toInt()),
                    children = listOf(
                        ImageNode(
                            source = R.drawable.ic_microphone_black_24dp.toBuilder()
                                .addTransform(ColorFilter(contentColor))
                                .build(),
                            layoutWidth = LayoutDimension.Fixed(20.dp().toInt()),
                            layoutHeight = LayoutDimension.Fixed(20.dp().toInt()),
                        ).linearChild(),
                        TextNode(
                            text = text,
                            maxLines = 1,
                        ).linearChild(),
                    )
                ),
                startToStartOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
            )
        )
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        text to "text",
        contentColor to "contentColor"
    )
}
