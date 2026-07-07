package com.simple.phonetics.ui.base.adapters

import com.simple.adapter.annotation.ItemAdapter
import com.simple.phonetics.ui.common.adapters.PrecomputeAdapter
import com.simple.phonetics.ui.common.adapters.PrecomputeViewItem
import com.simple.phonetics.utils.exts.dp
import com.simple.ui.precompute.node.CrossAlign
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.FlexAlignContent
import com.simple.ui.precompute.node.FlexAlignItems
import com.simple.ui.precompute.node.FlexChild
import com.simple.ui.precompute.node.FlexDirection
import com.simple.ui.precompute.node.FlexJustifyContent
import com.simple.ui.precompute.node.FlexWrap
import com.simple.ui.precompute.node.FlexboxNode
import com.simple.ui.precompute.node.GridNode
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.linearChild

@ItemAdapter
class GridAdapter : PrecomputeAdapter<GridViewItem>() {

    override val viewItemClass: Class<GridViewItem> by lazy {
        GridViewItem::class.java
    }
}

data class GridViewItem(
    override val id: String,
    override val maxWidth: Int,

    val column: Int,
    val viewItems: List<PrecomputeViewItem>
) : PrecomputeViewItem() {

    override val node: LayoutNode = LinearNode(
        crossAlign = CrossAlign.CENTER,
        orientation = Orientation.VERTICAL,
        layoutWidth = LayoutDimension.MatchParent,
        children = listOfNotNull(
            FlexboxNode(
                flexWrap = FlexWrap.WRAP,
                flexDirection = FlexDirection.ROW,
                alignItems = FlexAlignItems.FLEX_START,
                alignContent = FlexAlignContent.FLEX_START,
                justifyContent = FlexJustifyContent.FLEX_START,
                layoutWidth = LayoutDimension.MatchParent,
                children = viewItems.mapIndexed { index, item ->
                    FlexChild(
                        node = item.node
                    )
                }
            ).linearChild()
        )
    )

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        viewItems to "viewItems"
    )
}
