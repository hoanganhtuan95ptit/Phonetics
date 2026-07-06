package com.simple.phonetics.ui.view.node

import android.graphics.Color
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import com.simple.phonetics.ui.view.ReadingViewModel
import com.simple.phonetics.utils.exts.dp
import com.simple.phonetics.utils.exts.value
import com.simple.state.ResultState
import com.simple.state.isStart
import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.MeasureContext
import com.simple.ui.precompute.image.BigImage
import com.simple.ui.precompute.image.emptyImage
import com.simple.ui.precompute.node.BackgroundNode
import com.simple.ui.precompute.node.ConstraintChild
import com.simple.ui.precompute.node.ConstraintNode
import com.simple.ui.precompute.node.Constraints
import com.simple.ui.precompute.node.CrossAlign
import com.simple.ui.precompute.node.EdgeInsets
import com.simple.ui.precompute.node.GroupNode
import com.simple.ui.precompute.node.ImageNode
import com.simple.ui.precompute.node.LayoutDimension
import com.simple.ui.precompute.node.LayoutNode
import com.simple.ui.precompute.node.LinearNode
import com.simple.ui.precompute.node.Orientation
import com.simple.ui.precompute.node.LoadingSpec
import com.simple.ui.precompute.node.OutlineState
import com.simple.ui.precompute.node.TextNode
import com.simple.ui.precompute.node.linearChild
import com.simple.ui.precompute.node.resolve
import com.simple.ui.precompute.text.BigText
import com.simple.ui.precompute.text.emptyText
import com.unknown.coroutines.launchCollect

data class PhoneticBackgroundNode(

    override val id: String = "",
    val logicId: String = "", // ID dùng để so khớp với ReadingViewModel
    val text: String = "",

    val strokeShow: Boolean = false,
    val onlyReading: Boolean = false,
    val strokeColor: Int = Color.BLACK,
    val strokeWidth: Float = 1f,
    val cornerRadius: Float = 0f,
    val dashWidth: Float = 0f,
    val dashGap: Float = 0f,
    val loadingSegmentRatio: Float = 0.5f,
    val loadingDurationMs: Long = 1200L,
    val state: OutlineState = OutlineState.IDLE,
    override val padding: EdgeInsets = EdgeInsets.ZERO,
    override val layoutWidth: LayoutDimension = LayoutDimension.WrapContent,
    override val layoutHeight: LayoutDimension = LayoutDimension.WrapContent
) : LayoutNode() {

    override fun measure(
        ctx: MeasureContext,
        c: Constraints,
        x: Int,
        y: Int
    ): LoadingSpec {
        val p = padding
        val naturalW = p.horizontal
        val naturalH = p.vertical
        val w = layoutWidth.resolve(naturalW, c.maxWidth)
        val h = layoutHeight.resolve(naturalH, c.maxHeight)

        return PhoneticBackgroundSpec(
            id = if (logicId.isNotEmpty()) logicId else id,
            text = text,
            strokeShow = strokeShow,
            onlyReading = onlyReading,
            left = x,
            top = y,
            width = w,
            height = h,
            padding = p,
            strokeColor = strokeColor,
            strokeWidth = strokeWidth,
            cornerRadius = cornerRadius,
            dashWidth = dashWidth,
            dashGap = dashGap,
            loadingSegmentRatio = loadingSegmentRatio,
            loadingDurationMs = loadingDurationMs,
            state = state,
            node = this
        )
    }
}

class PhoneticBackgroundSpec(

    val id: String = "",
    val text: String = "",

    val strokeShow: Boolean = false,
    val onlyReading: Boolean = false,

    left: Int,
    top: Int,
    width: Int,
    height: Int,
    padding: EdgeInsets,
    strokeColor: Int,
    strokeWidth: Float,
    cornerRadius: Float,
    dashWidth: Float,
    dashGap: Float,
    loadingSegmentRatio: Float,
    loadingDurationMs: Long,
    state: OutlineState,
    node: LayoutNode
) : LoadingSpec(
    left, top, width, height, padding, strokeColor, strokeWidth, cornerRadius, dashWidth, dashGap, loadingSegmentRatio, loadingDurationMs, state, node
) {

    private var isInteractionEnabled: Boolean = true

    private lateinit var viewModel: ReadingViewModel

    init {

        onClick = {
            Log.d("tuanha", "PhoneticBackgroundSpec: ")
        }
    }

    override fun onAttachedToWindow(view: View) {
        super.onAttachedToWindow(view)

        val lifecycleOwner = view.findViewTreeLifecycleOwner() ?: return
        val viewModelStoreOwner = view.findViewTreeViewModelStoreOwner() ?: return

        viewModel = ViewModelProvider(viewModelStoreOwner)[ReadingViewModel::class.java]


        viewModel.readingState.launchCollect(lifecycleOwner) {

            val state = if (it.first == id) {
                it.second
            } else {
                ResultState.Idle
            }

            setLoading(state.isStart(), show = strokeShow || state.isStart(), animate = true)

            isInteractionEnabled = !state.isStart() && viewModel.isSupportReadingFlow.value == true
        }

        viewModel.isSupportReadingFlow.launchCollect(lifecycleOwner) {

            isInteractionEnabled = it
        }
    }

    override fun onDetachedFromWindow(view: View) {
        super.onDetachedFromWindow(view)
    }

    override fun copyTo(newLeft: Int, newTop: Int, newWidth: Int, newHeight: Int): DrawSpec = PhoneticBackgroundSpec(
        id = id,
        text = text,
        strokeShow = strokeShow,
        onlyReading = onlyReading,
        left = newLeft,
        top = newTop,
        width = newWidth,
        height = newHeight,
        padding = padding,
        strokeColor = strokeColor,
        strokeWidth = strokeWidth,
        cornerRadius = cornerRadius,
        dashWidth = dashWidth,
        dashGap = dashGap,
        loadingSegmentRatio = loadingSegmentRatio,
        loadingDurationMs = loadingDurationMs,
        state = state,
        node = node
    ).also {
        it.internalState = internalState
        it.settledInternalState = settledInternalState
        it.tailPos = tailPos
        it.segLen = segLen
        it.targetSegLen = targetSegLen
        it.onClick = onClick
    }
}

data class PhoneticNode(
    override val id: String = "",
    var text: String = "",
    var contentColor: Int = 0,

    var strokeShow: Boolean = false,
    var onlyReading: Boolean = false,

    val textDisplay: BigText = emptyText(),
    val phoneticDisplay: BigText = emptyText(),

    val iconShow: Boolean = false,
    val iconDisplay: BigImage = emptyImage(),
) : GroupNode() {

    override fun buildChildren(): List<LayoutNode> = ConstraintNode(
        children = listOf(
            ConstraintChild(
                id = "outline",
                node = BackgroundNode(
//                    id = "${id}_outline",
//                    logicId = id,
//                    text = text,
//                    strokeShow = strokeShow,
//                    onlyReading = onlyReading,
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
                    padding = EdgeInsets.symmetric(h = 8.dp().toInt(), v = 4.dp().toInt()),
                    crossAlign = CrossAlign.START,
                    orientation = Orientation.VERTICAL,
                    layoutWidth = layoutWidth,
                    layoutHeight = layoutHeight,
                    children = listOf(
                        LinearNode(
                            gap = 4.dp().toInt(),
                            crossAlign = CrossAlign.CENTER,
                            orientation = Orientation.HORIZONTAL,
                            children = listOfNotNull(
                                TextNode(
                                    text = textDisplay,
                                ).linearChild(),
                                ImageNode(
                                    source = iconDisplay,
                                    layoutWidth = LayoutDimension.Fixed(8.dp().toInt()),
                                    layoutHeight = LayoutDimension.Fixed(8.dp().toInt()),
                                ).takeIf {
                                    iconShow
                                }?.linearChild(),
                            ),
                        ).linearChild(),
                        // Hàng dưới: phiên âm
                        TextNode(
                            text = phoneticDisplay,
                        ).linearChild(),
                    ),
                ),
                startToStartOf = ConstraintNode.PARENT,
                topToTopOf = ConstraintNode.PARENT,
            )
        )
    ).let {

        listOf(it)
    }
}
/*

*/
/**
 * DrawSpec bọc quanh [inner] (kết quả measure của cây node bên trong):
 *  - Uỷ quyền kích thước + vẽ cho [inner]
 *  - Gắn thêm vòng đời view để observe [ReadingViewModel]:
 *      + Đổi trạng thái loading + strokeShow theo readingState
 *      + Bật/tắt click theo isSupportReadingFlow
 *//*

class PhoneticSpec(
    val id: String = "",
    val text: String = "",

    val strokeShow: Boolean = false,
    val onlyReading: Boolean = false,

    override val left: Int,
    override val top: Int,
    override val width: Int,
    override val height: Int,
    override val node: LayoutNode,

    override val children: List<DrawSpec>,
) : GroupSpec(left, top, width, height, node, children) {

    override fun withPosition(newLeft: Int, newTop: Int): DrawSpec {
        if (newLeft == left && newTop == top) return this
        val dx = newLeft - left
        val dy = newTop - top
        val shiftedChildren = children.map { it.withPosition(it.left + dx, it.top + dy) }
        return PhoneticSpec(id, text, strokeShow, onlyReading, newLeft, newTop, width, height, node, shiftedChildren)
    }

    private var isInteractionEnabled: Boolean = true

    private lateinit var viewModel: ReadingViewModel

    init {

//        Log.d("tuanha", "init: ${this}")
//        onClick = {
//
//            Log.d("tuanha", "onClick: ${this} isInteractionEnabled:$isInteractionEnabled onlyReading:$onlyReading id:$id text:$text viewModel:${viewModel}")
//            if (isInteractionEnabled) if (onlyReading) {
//                viewModel.startReading(id, text)
//            } else {
//                sendDeeplink(DeeplinkManager.SPEAK, extras = mapOf(Param.TEXT to text))
//            }
//        }
    }

    override fun onAttachedToWindow(view: View) {
        super.onAttachedToWindow(view)

//        val lifecycleOwner = view.findViewTreeLifecycleOwner() ?: return
//        val viewModelStoreOwner = view.findViewTreeViewModelStoreOwner() ?: return
//
//        viewModel = ViewModelProvider(viewModelStoreOwner)[ReadingViewModel::class.java]
//
//        val host = findById("outline_1").asObjectOrNull<OutlineSpec>()
//        Log.d("tuanha", "onAttachedToWindow: ${this@PhoneticSpec} host:$host")
//
//        viewModel.readingState.launchCollect(lifecycleOwner) {
//
//            val state = if (it.first == id) {
//                it.second
//            } else {
//                ResultState.Idle
//            }
//
//            host?.setLoading(state.isStart(), show = strokeShow || state.isStart(), animate = true)
//
//            isInteractionEnabled = !state.isStart() && viewModel.isSupportReadingFlow.value == true
//        }
//
//        viewModel.isSupportReadingFlow.launchCollect(lifecycleOwner) {
//
//            isInteractionEnabled = it
//        }
    }
}
*/
