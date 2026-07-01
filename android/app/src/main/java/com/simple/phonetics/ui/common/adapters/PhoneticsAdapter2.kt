package com.simple.phonetics.ui.common.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.ext.DP
import com.simple.phonetics.databinding.ItemPhonetics2Binding
import com.simple.ui.precompute.LayoutEngine
import com.simple.ui.precompute.image.BigImage
import com.simple.ui.precompute.image.emptyImage
import com.simple.ui.precompute.node.ConstraintChild
import com.simple.ui.precompute.node.ConstraintNode
import com.simple.ui.precompute.node.Constraints
import com.simple.ui.precompute.node.CrossAlign
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.ImageNode
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.emptyText


@ItemAdapter
class PhoneticsAdapter2(onItemClick: ((View, PhoneticsViewItem2) -> Unit)? = null) : ViewItemAdapter<PhoneticsViewItem2, ItemPhonetics2Binding>(onItemClick) {

    override val viewItemClass: Class<PhoneticsViewItem2> by lazy {
        PhoneticsViewItem2::class.java
    }

    override fun createViewBinding(parent: ViewGroup, viewType: Int): ItemPhonetics2Binding {
        return ItemPhonetics2Binding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

//    override fun createViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<ItemPhonetics2Binding>? {
//        val viewHolder = super.createViewHolder(parent, viewType) ?: return null
//
//        viewHolder.itemView.setDebouncedClickListener {
//
//            val item = getViewItem(viewHolder.absoluteAdapterPosition) ?: return@setDebouncedClickListener
//
//            sendEvent("PHONETIC_CLICKED", item)
//        }
//
//        return viewHolder
//    }

    override fun onBindViewHolder(binding: ItemPhonetics2Binding, viewType: Int, position: Int, item: PhoneticsViewItem2, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        binding.root.id = item.id
        binding.root.text = item.text

        if (payloads.contains("hasStroke")) hasStroke(binding, item, animate = true)
        if (payloads.contains("textDisplay")) textDisplay(binding, item)
        if (payloads.contains("strokeColor")) strokeColor(binding, item)
    }

    override fun onBindViewHolder(binding: ItemPhonetics2Binding, viewType: Int, position: Int, item: PhoneticsViewItem2) {
        super.onBindViewHolder(binding, viewType, position, item)

        binding.root.id = item.id
        binding.root.text = item.text

        hasStroke(binding, item)
        textDisplay(binding, item)
        strokeColor(binding, item)
    }

    private fun hasStroke(binding: ItemPhonetics2Binding, item: PhoneticsViewItem2, animate: Boolean = false) {
        binding.root.hasStroke = item.hasStroke
        binding.root.setLoading(false, item.hasStroke, animate)
    }

    private fun textDisplay(binding: ItemPhonetics2Binding, item: PhoneticsViewItem2) {
        binding.root.spec = item.drawSpec
    }

    private fun strokeColor(binding: ItemPhonetics2Binding, item: PhoneticsViewItem2) {
        binding.root.strokeColor = item.strokeColor
    }
}

data class PhoneticsViewItem2(
    val id: String,
    val text: String,

    val textDisplay: BigText = emptyText(),

    val iconShow: Boolean = false,
    val iconDisplay: BigImage = emptyImage(),

    val hasStroke: Boolean = false,
    val strokeColor: Int = Color.TRANSPARENT,

    val maxWidth: Int,
) : ViewItem {

    val drawSpec = ConstraintNode(
        children = listOf(
//            ConstraintChild(
//                id = "bg",
//                node = OutlineNode(
//                    state = outlineState,
//                    strokeColor = outlineStrokeColor,
//                    cornerRadius = DP.DP_8.toFloat(),
//                    strokeWidth = DP.DP_1.toFloat(),
//                    layoutWidth = LayoutDimension.MatchParent,
//                    layoutHeight = LayoutDimension.MatchParent,
//                ),
//                startToStartOf = "content",
//                endToEndOf = "content",
//                topToTopOf = "content",
//                bottomToBottomOf = "content",
//                width = LayoutDimension.MatchParent,
//                height = LayoutDimension.MatchParent,
//            ),
            ConstraintChild(
                id = "content",
                node = LinearNode(
                    orientation = Orientation.HORIZONTAL,
                    crossAlign = CrossAlign.CENTER,
                    gap = DP.DP_4, // marginStart của ImageView
                    padding = EdgeInsets.symmetric(h = DP.DP_8, v = DP.DP_4),
                    children = listOfNotNull(
                        TextNode(
                            text = textDisplay,
                        ),
                        if (iconShow) ImageNode(
                            source = iconDisplay,
                            layoutWidth = LayoutDimension.Fixed(DP.DP_10),
                            layoutHeight = LayoutDimension.Fixed(DP.DP_24),
                        ) else null,
                    ),
                ),
                startToStartOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
            ),
        )
    ).let {
        LayoutEngine.measure(it, Constraints(maxWidth))
    }

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        textDisplay to "textDisplay",
        iconShow to "textDisplay",
        iconDisplay to "textDisplay",

        hasStroke to "hasStroke",
        strokeColor to "strokeColor",
    )
}
