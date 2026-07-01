package com.simple.feature.pronunciation_assessment.ui.adapters

import com.simple.adapter.annotation.ItemAdapter
import com.simple.phonetics.ui.common.adapters.PrecomputeAdapter
import com.simple.phonetics.ui.common.adapters.PrecomputeViewItem
import com.simple.phonetics.utils.exts.dp
import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.LayoutEngine
import com.simple.ui.precompute.image.BigImage
import com.simple.ui.precompute.image.emptyImage
import com.simple.ui.precompute.node.BackgroundData
import com.simple.ui.precompute.node.BackgroundNode
import com.simple.ui.precompute.node.ConstraintChild
import com.simple.ui.precompute.node.ConstraintNode
import com.simple.ui.precompute.node.Constraints
import com.simple.ui.precompute.node.CrossAlign
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.ImageNode
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.SpaceNode
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.emptyText

@ItemAdapter
class NoteAdapter : PrecomputeAdapter<NoteViewItem>() {

    override val viewItemClass: Class<NoteViewItem> by lazy {
        NoteViewItem::class.java
    }
}

data class NoteViewItem(
    override val id: String,
    override val maxWidth: Int,

    val note: BigText = emptyText(),
    val title: BigText = emptyText(),
    val image: BigImage = emptyImage(),

    val background: BackgroundData = BackgroundData()
) : PrecomputeViewItem() {

    override val drawSpec: DrawSpec = ConstraintNode(
        children = listOf(
            ConstraintChild(
                id = "bg",
                node = BackgroundNode(
                    cornerRadius = background.cornerRadius,
                    strokeColor = background.strokeColor,
                    strokeWidth = background.strokeWidth,
                    dashGap = background.dashGap,
                    dashWidth = background.dashWidth,
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
                    crossAlign = CrossAlign.START,
                    padding = EdgeInsets.symmetric(h = 16.dp().toInt(), v = 18.dp().toInt()),
                    layoutWidth = LayoutDimension.MatchParent,
                    children = listOf(
                        ImageNode(
                            source = image,
                            layoutWidth = LayoutDimension.Fixed(28.dp().toInt()),
                            layoutHeight = LayoutDimension.Fixed(28.dp().toInt())
                        ),
                        SpaceNode.horizontal(16.dp().toInt()),
                        LinearNode(
                            orientation = Orientation.VERTICAL,
                            layoutWidth = LayoutDimension.MatchParent,
                            children = listOf(
                                TextNode(
                                    text = title,
                                    layoutWidth = LayoutDimension.MatchParent
                                ),
                                SpaceNode.vertical(8.dp().toInt()),
                                TextNode(
                                    text = note,
                                    layoutWidth = LayoutDimension.MatchParent
                                )
                            )
                        )
                    )
                ),
                startToStartOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
            ),
        )
    ).let {

        LayoutEngine.measure(node = it, constraints = Constraints(maxWidth), id = id)
    }


    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        note to "note",
        title to "title",
        image to "image",
        background to "background"
    )
}
