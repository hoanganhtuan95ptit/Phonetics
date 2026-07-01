package com.simple.phonetics.ui.common.adapters

import android.util.Log
import com.simple.adapter.annotation.ItemAdapter
import com.simple.ui.precompute.LayoutEngine
import com.simple.ui.precompute.node.ConstraintChild
import com.simple.ui.precompute.node.ConstraintNode
import com.simple.ui.precompute.node.Constraints
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutDimension.Companion.toLayoutDimension
import com.simple.ui.precompute.node.SpaceNode

@ItemAdapter
class SpaceAdapter : PrecomputeAdapter<SpaceViewItem2>() {

    override val viewItemClass: Class<SpaceViewItem2> by lazy {
        SpaceViewItem2::class.java
    }
}

fun SpaceViewItem2(
    id: String,
    maxWidth: Int,

    width: Float = -1f,
    height: Float = -1f,
) = SpaceViewItem2(
    id = id,
    maxWidth = maxWidth,
    width = width.toInt().toLayoutDimension(),
    height = height.toInt().toLayoutDimension(),
)

data class SpaceViewItem2(
    override val id: String,
    override val maxWidth: Int,

    val width: LayoutDimension = LayoutDimension.MatchParent,
    val height: LayoutDimension = LayoutDimension.MatchParent,
) : PrecomputeViewItem() {

    override val drawSpec = ConstraintNode(
        children = listOf(
            ConstraintChild(
                id = "content",
                node = SpaceNode(
                    layoutWidth = width,
                    layoutHeight = height,
                ),
                startToStartOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
            ),
        )
    ).let {

        LayoutEngine.measure(node = it, constraints = Constraints(maxWidth), id = id)
    }.apply {

        Log.d("tuanha", "drawSpec: $this")
    }


    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        width to "width",
        height to "height",
    )
}
