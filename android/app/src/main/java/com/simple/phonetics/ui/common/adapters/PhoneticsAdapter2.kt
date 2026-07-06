package com.simple.phonetics.ui.common.adapters

import android.graphics.Color
import com.simple.adapter.annotation.ItemAdapter
import com.simple.phonetics.ui.view.node.PhoneticNode
import com.simple.ui.precompute.LayoutEngine
import com.simple.ui.precompute.image.BigImage
import com.simple.ui.precompute.image.emptyImage
import com.simple.ui.precompute.node.Constraints
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.emptyText


@ItemAdapter
class PhoneticsAdapter2 : PrecomputeAdapter<PhoneticsViewItem2>() {

    override val viewItemClass: Class<PhoneticsViewItem2> by lazy {
        PhoneticsViewItem2::class.java
    }
}

data class PhoneticsViewItem2(
    override val id: String,
    override val maxWidth: Int,

    val text: String,

    val textDisplay: BigText = emptyText(),
    val phoneticDisplay: BigText = emptyText(),

    val iconShow: Boolean = false,
    val iconDisplay: BigImage = emptyImage(),

    val onlyReading: Boolean = false,

    val strokeShow: Boolean = false,
    val strokeColor: Int = Color.TRANSPARENT,
) : PrecomputeViewItem() {

    override val node: LayoutNode = PhoneticNode(
        id = id,
        text = text,

        strokeShow = strokeShow,
        onlyReading = onlyReading,
        contentColor = strokeColor,

        textDisplay = textDisplay,
        phoneticDisplay = phoneticDisplay,

        iconShow = iconShow,
        iconDisplay = iconDisplay,
    )

    override fun areItemsTheSame(): List<Any> = listOf(
        id
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        textDisplay to "drawSpec",
        phoneticDisplay to "drawSpec",

        iconShow to "drawSpec",
        iconDisplay to "drawSpec",

        onlyReading to "drawSpec",

        strokeShow to "drawSpec",
        strokeColor to "drawSpec",
    )
}
