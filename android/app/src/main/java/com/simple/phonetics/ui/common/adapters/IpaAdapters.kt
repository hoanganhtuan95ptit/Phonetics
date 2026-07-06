package com.simple.phonetics.ui.common.adapters

import android.view.View
import android.view.ViewGroup
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.base.BaseBindingViewHolder
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.DEFAULT_BACKGROUND
import com.simple.coreapp.ui.view.DEFAULT_MARGIN
import com.simple.coreapp.ui.view.DEFAULT_SIZE
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.Size
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.event.sendEvent
import com.simple.ipa.entities.Ipa
import com.simple.phonetics.EventName
import com.simple.phonetics.databinding.ItemPrecomputeBinding
import com.simple.phonetics.ui.base.adapters.SizeViewItem
import com.simple.phonetics.ui.base.adapters.measureTextViewHeight
import com.simple.phonetics.utils.TextViewMetrics
import com.simple.phonetics.utils.exts.dp
import com.simple.phonetics.utils.exts.sp
import com.simple.ui.precompute.node.BackgroundNode
import com.simple.ui.precompute.node.ConstraintChild
import com.simple.ui.precompute.node.ConstraintNode
import com.simple.ui.precompute.node.CrossAlign
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.SpaceNode
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.node.linearChild
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.emptyText
import com.unknown.size.uitls.exts.width

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

    override var size: Size = DEFAULT_SIZE,
    val margin: Margin = DEFAULT_MARGIN,
    val padding: Padding = Padding(paddingVertical = DP.DP_16),
    val background: Background = DEFAULT_BACKGROUND
) : PrecomputeViewItem(), SizeViewItem {

    private var ipaMetrics: TextViewMetrics? = null
    private var textMetrics: TextViewMetrics? = null

    override val node: LayoutNode by lazy {
        ConstraintNode(
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
                    startToStartOf = ConstraintNode.PARENT,
                    endToEndOf = ConstraintNode.PARENT,
                    topToTopOf = ConstraintNode.PARENT,
                    bottomToBottomOf = ConstraintNode.PARENT,
                ),
                ConstraintChild(
                    id = "content",
                    node = LinearNode(
                        orientation = Orientation.VERTICAL,
                        crossAlign = CrossAlign.CENTER,
                        padding = EdgeInsets(
                            left = padding.left,
                            top = padding.top,
                            right = padding.right,
                            bottom = padding.bottom
                        ),
                        layoutWidth = LayoutDimension.MatchParent,
                        children = listOf(
                            TextNode(
                                text = ipa,
                                textSizePx = ipaMetrics?.textSizePx ?: 20.sp(),
                                typeface = ipaMetrics?.typeface,
                                layoutWidth = LayoutDimension.WrapContent
                            ).linearChild(),
                            SpaceNode.vertical(4.dp().toInt()).linearChild(),
                            TextNode(
                                text = text,
                                textSizePx = textMetrics?.textSizePx ?: 14.sp(),
                                typeface = textMetrics?.typeface,
                                layoutWidth = LayoutDimension.WrapContent
                            ).linearChild()
                        )
                    ),
                    startToStartOf = ConstraintNode.PARENT,
                    endToEndOf = ConstraintNode.PARENT,
                    topToTopOf = ConstraintNode.PARENT,
                    bottomToBottomOf = ConstraintNode.PARENT,
                )
            )
        )
    }

    override fun measureSize(appSize: Map<String, Int>, style: Map<String, TextViewMetrics>): Size {

        val ipaMetrics = style["TextAppearance_MaterialComponents_Headline6"] ?: return this.size
        val textMetrics = style["TextAppearance_MaterialComponents_Body1"] ?: return this.size

        this.ipaMetrics = ipaMetrics
        this.textMetrics = textMetrics

        val innerMaxWidth = if (this.size.width < 10) {
            appSize.width - padding.left - padding.right - margin.left - margin.right
        } else {
            this.size.width - padding.left - padding.right
        }

        val ipaHeight = measureTextViewHeight(
            text = ipa.textChar,
            maxWidth = innerMaxWidth,
            metrics = ipaMetrics
        )

        val textHeight = measureTextViewHeight(
            text = text.textChar,
            maxWidth = innerMaxWidth,
            metrics = textMetrics
        )

        return Size(
            width = if (this.size.width < 10) innerMaxWidth else this.size.width,
            height = ipaHeight + 4.dp().toInt() + textHeight + padding.top + padding.bottom
        ).also {
            this.size = it
        }
    }

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        ipa to "ipa",
        text to "text",

        size to "size",
        margin to "margin",
        background to "background"
    )
}
