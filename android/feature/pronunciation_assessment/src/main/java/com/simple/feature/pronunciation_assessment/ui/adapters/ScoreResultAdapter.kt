package com.simple.feature.pronunciation_assessment.ui.adapters

import android.graphics.Typeface
import com.simple.adapter.annotation.ItemAdapter
import com.simple.coreapp.utils.ext.DP
import com.simple.phonetics.ui.common.adapters.PrecomputeAdapter
import com.simple.phonetics.ui.common.adapters.PrecomputeViewItem
import com.simple.phonetics.utils.exts.dp
import com.simple.phonetics.utils.exts.sp
import com.simple.phonetics.utils.exts.toPronunciationColor
import com.simple.ui.precompute.LayoutEngine
import com.simple.ui.precompute.LayoutResult
import com.simple.ui.precompute.node.ConstraintChild
import com.simple.ui.precompute.node.ConstraintNode
import com.simple.ui.precompute.node.Constraints
import com.simple.ui.precompute.node.CrossAlign
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.GaugeArcNode
import com.simple.ui.precompute.node.GaugeScoreNode
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.LinearChild
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.ProgressBarNode
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.node.linearChild
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.emptyText

@ItemAdapter
class ScoreResultAdapter : PrecomputeAdapter<ScoreResultViewItem>() {

    override val viewItemClass: Class<ScoreResultViewItem> by lazy {
        ScoreResultViewItem::class.java
    }
}

data class ScoreResultViewItem(
    override val id: String,
    override val maxWidth: Int = 0,

    val score: Int = 0,
    val label: String = "",
    val grade: String = "",
    val subtitle: BigText = emptyText(),

    val accuracy: Int = 0,
    val accuracyTitle: BigText = emptyText(),
    val accuracyValue: BigText = emptyText(),

    val completion: Int = 0,
    val completionTitle: BigText = emptyText(),
    val completionValue: BigText = emptyText(),

    val fluency: Int = 0,
    val fluencyTitle: BigText = emptyText(),
    val fluencyValue: BigText = emptyText(),
) : PrecomputeViewItem() {

    private val gaugeSize = 160.dp().toInt()
    private val textSizePx = 13.sp()
    private val strokeWidthPx = 12.dp()
    private val progressHeight = 12.dp().toInt()

    override val node: LayoutNode by lazy {
        LinearNode(
            orientation = Orientation.VERTICAL,
            crossAlign = CrossAlign.CENTER,
            layoutWidth = LayoutDimension.MatchParent,
            children = listOf(
                buildTopRow().linearChild(),
                buildSubtitleNode().linearChild()
            ),
        )
    }

    private fun buildTopRow(): LayoutNode {

        val gauge = buildGauge()
        val metricsColumn = buildMetricsColumn()
        val children = listOf(gauge.linearChild(), metricsColumn.linearChild())
        return LinearNode(
            orientation = Orientation.HORIZONTAL,
            crossAlign = CrossAlign.CENTER,
            gap = DP.DP_16,
            layoutWidth = LayoutDimension.MatchParent,
            children = children,
        )
    }

    private fun buildGauge(): ConstraintNode {

        val arcChild = buildGaugeArcChild()
        val scoreChild = buildGaugeScoreChild()
        val children = listOf(arcChild, scoreChild)

        return ConstraintNode(
            children = children,
            layoutWidth = LayoutDimension.Fixed(gaugeSize),
            layoutHeight = LayoutDimension.Fixed(gaugeSize),
        )
    }

    private fun buildGaugeArcChild(): ConstraintChild {

        val node = GaugeArcNode(
            progress = score,
            progressColor = score.toPronunciationColor(),
            strokeWidthPx = strokeWidthPx,
            layoutWidth = LayoutDimension.MatchParent,
            layoutHeight = LayoutDimension.MatchParent,
        )
        return ConstraintChild(
            id = "arc",
            node = node,
            startToStartOf = ConstraintNode.PARENT,
            endToEndOf = ConstraintNode.PARENT,
            topToTopOf = ConstraintNode.PARENT,
            bottomToBottomOf = ConstraintNode.PARENT,
            width = LayoutDimension.MatchParent,
            height = LayoutDimension.MatchParent,
        )
    }

    private fun buildGaugeScoreChild(): ConstraintChild {

        val node = GaugeScoreNode(
            progress = score,
            label = label,
            grade = grade,
            gradeColor = score.toPronunciationColor(),
            layoutWidth = LayoutDimension.MatchParent,
            layoutHeight = LayoutDimension.MatchParent,
        )
        return ConstraintChild(
            id = "score",
            node = node,
            startToStartOf = ConstraintNode.PARENT,
            endToEndOf = ConstraintNode.PARENT,
            topToTopOf = ConstraintNode.PARENT,
            bottomToBottomOf = ConstraintNode.PARENT,
            width = LayoutDimension.MatchParent,
            height = LayoutDimension.MatchParent,
        )
    }

    private fun buildMetricsColumn(): LayoutNode {

        val children = buildMetricRows()
        return LinearNode(
            orientation = Orientation.VERTICAL,
            layoutWidth = LayoutDimension.MatchParent,
            children = children,
        )
    }

    private fun buildMetricRows(): List<LinearChild> {

        val accuracyRow = buildMetric(
            title = accuracyTitle,
            value = accuracyValue,
            progress = accuracy,
            idSuffix = "accuracy",
            paddingBottom = DP.DP_12,
        )
        val completionRow = buildMetric(
            title = completionTitle,
            value = completionValue,
            progress = completion,
            idSuffix = "completion",
            paddingBottom = DP.DP_12,
        )
        val fluencyRow = buildMetric(
            title = fluencyTitle,
            value = fluencyValue,
            progress = fluency,
            idSuffix = "fluency",
            paddingBottom = DP.DP_4,
        )
        return listOf(accuracyRow.linearChild(), completionRow.linearChild(), fluencyRow.linearChild())
    }

    private fun buildMetric(
        title: BigText,
        value: BigText,
        progress: Int,
        idSuffix: String,
        paddingBottom: Int,
    ): LayoutNode {

        val header = buildMetricHeader(title, value, idSuffix)
        val progressBar = buildMetricProgressBar(progress)
        val padding = EdgeInsets.symmetric(v = 8.dp().toInt())
        val children = listOf(header.linearChild(), progressBar.linearChild())
        return LinearNode(
            orientation = Orientation.VERTICAL,
            gap = DP.DP_6,
            padding = padding,
            layoutWidth = LayoutDimension.MatchParent,
            children = children,
        )
    }

    private fun buildMetricHeader(
        title: BigText,
        value: BigText,
        idSuffix: String,
    ): ConstraintNode {

        val valueChild = buildMetricValueChild(value, idSuffix)
        val titleChild = buildMetricTitleChild(title, idSuffix)
        val children = listOf(valueChild, titleChild)
        return ConstraintNode(
            children = children,
            layoutWidth = LayoutDimension.MatchParent,
            layoutHeight = LayoutDimension.WrapContent,
        )
    }

    private fun buildMetricValueChild(value: BigText, idSuffix: String): ConstraintChild {

        val node = TextNode(
            text = value,
            textSizePx = textSizePx,
            typeface = Typeface.DEFAULT_BOLD,
            maxLines = 1,
        )
        return ConstraintChild(
            id = "value_$idSuffix",
            node = node,
            endToEndOf = ConstraintNode.PARENT,
            topToTopOf = ConstraintNode.PARENT,
            width = LayoutDimension.WrapContent,
            height = LayoutDimension.WrapContent,
        )
    }

    private fun buildMetricTitleChild(title: BigText, idSuffix: String): ConstraintChild {

        val node = TextNode(
            text = title,
            textSizePx = textSizePx,
            maxLines = 1,
        )
        return ConstraintChild(
            id = "title_$idSuffix",
            node = node,
            startToStartOf = ConstraintNode.PARENT,
            endToStartOf = "value_$idSuffix",
            topToTopOf = ConstraintNode.PARENT,
            width = LayoutDimension.MatchParent,
            height = LayoutDimension.WrapContent,
            horizontalBias = 0f,
        )
    }

    private fun buildMetricProgressBar(progress: Int): ProgressBarNode {

        return ProgressBarNode(
            progress = progress,
            progressColor = progress.toPronunciationColor(),
            layoutWidth = LayoutDimension.MatchParent,
            layoutHeight = LayoutDimension.Fixed(progressHeight),
        )
    }

    private fun buildSubtitleNode(): TextNode {

        val padding = EdgeInsets(top = DP.DP_16, bottom = DP.DP_16)

        return TextNode(
            text = subtitle,
            textSizePx = textSizePx,
            padding = padding,
        )
    }

    override fun getContentsCompare(): List<Pair<Any, String>> = listOf(
        score to "drawSpec",
        label to "drawSpec",
        grade to "drawSpec",
        subtitle to "drawSpec",
        accuracy to "drawSpec",
        accuracyTitle to "drawSpec",
        accuracyValue to "drawSpec",
        completion to "drawSpec",
        completionTitle to "drawSpec",
        completionValue to "drawSpec",
        fluency to "drawSpec",
        fluencyTitle to "drawSpec",
        fluencyValue to "drawSpec",
        maxWidth to "drawSpec",
    )
}
