package com.simple.phonetics.ui.common.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simple.adapter.ViewItemAdapter
import com.simple.adapter.annotation.ItemAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.phonetics.databinding.ItemPhonetics2Binding
import com.simple.phonetics.utils.exts.dp
import com.simple.ui.precompute.LayoutEngine
import com.simple.ui.precompute.image.BigImage
import com.simple.ui.precompute.image.emptyImage
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

    override fun onBindViewHolder(binding: ItemPhonetics2Binding, viewType: Int, position: Int, item: PhoneticsViewItem2) {
        super.onBindViewHolder(binding, viewType, position, item)
        onBindViewHolder(binding, viewType, position, item, mutableListOf())
    }

    override fun onBindViewHolder(binding: ItemPhonetics2Binding, viewType: Int, position: Int, item: PhoneticsViewItem2, payloads: MutableList<Any>) {
        super.onBindViewHolder(binding, viewType, position, item, payloads)

        binding.root.id = item.id
        binding.root.text = item.text
        binding.root.onlyReading = item.onlyReading

        if (payloads.isEmpty() || payloads.contains("strokeShow")) strokeShow(binding, item, animate = payloads.isNotEmpty())
        if (payloads.isEmpty() || payloads.contains("drawSpec")) drawSpec(binding, item)
        if (payloads.isEmpty() || payloads.contains("strokeColor")) strokeColor(binding, item)
    }


    private fun strokeShow(binding: ItemPhonetics2Binding, item: PhoneticsViewItem2, animate: Boolean = false) {
        binding.root.strokeShow = item.strokeShow
        binding.root.setLoading(false, item.strokeShow, animate)
    }

    private fun drawSpec(binding: ItemPhonetics2Binding, item: PhoneticsViewItem2) {
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
    val phoneticDisplay: BigText = emptyText(),

    val iconShow: Boolean = false,
    val iconDisplay: BigImage = emptyImage(),

    val onlyReading: Boolean = false,

    val strokeShow: Boolean = false,
    val strokeColor: Int = Color.TRANSPARENT,

    val maxWidth: Int,
) : ViewItem {

    val drawSpec = LinearNode(
        orientation = Orientation.VERTICAL,
        crossAlign = CrossAlign.START,
        padding = EdgeInsets(left = 4.dp().toInt(), right = 8.dp().toInt(), bottom = 8.dp().toInt()),
        children = listOf(
            LinearNode(
                orientation = Orientation.HORIZONTAL,
                crossAlign = CrossAlign.CENTER,
                gap = 8.dp().toInt(),
                children = listOfNotNull(
                    TextNode(
                        text = textDisplay,
                    ),
                    ImageNode(
                        source = iconDisplay,
                        layoutWidth = LayoutDimension.Fixed(8.dp().toInt()),
                        layoutHeight = LayoutDimension.Fixed(8.dp().toInt()),
                    ).takeIf {
                        iconShow
                    },
                ),
            ),
            TextNode(
                text = phoneticDisplay,
            )
        ),
    ).let {

        LayoutEngine.measure(id = id, node = it, constraints = Constraints(maxWidth))
    }

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        textDisplay to "drawSpec",
        phoneticDisplay to "drawSpec",
        iconShow to "drawSpec",
        iconDisplay to "drawSpec",

        strokeShow to "strokeShow",
        strokeColor to "strokeColor",
    )
}
