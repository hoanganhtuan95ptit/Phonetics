package com.simple.phonetics.utils.view.shape

import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.view.ViewGroup
import kotlin.math.abs

interface ShapeOfView {

    val path: Path

    var rectF: RectF

    var topLeftRadius: Float
    var topRightRadius: Float
    var bottomRightRadius: Float
    var bottomLeftRadius: Float

    fun refresh(force: Boolean = false) {

        if (this !is View) {
            return
        }

        if (height <= 0 || width <= 0 || topLeftRadius <= 0 || topRightRadius <= 0 || bottomRightRadius <= 0 || bottomLeftRadius <= 0) {
            return
        }

        val rectF = RectF(0f, 0f, width - 0f, height - 0f)


        if (rectF == this.rectF && !force) return
        this.rectF = rectF


        path.reset()

        if (this is ViewGroup) {

            path.addRect(0f, 0f, 1f * getWidth(), 1f * getHeight(), Path.Direction.CW)
            path.op(generatePath(false, this.rectF, topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius), Path.Op.DIFFERENCE)
        } else {

            path.addPath(generatePath(false, this.rectF, topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius))
        }

        if (force) {

            postInvalidate()
        }
    }

    fun setRadius(radius: Float) {

        if (this !is View) return

        setRadius(radius, radius, radius, radius)
    }

    fun setRadius(topLeftRadius: Float, topRightRadius: Float, bottomRightRadius: Float, bottomLeftRadius: Float) {

        if (this !is View) return

        this.topLeftRadius = topLeftRadius
        this.topRightRadius = topRightRadius
        this.bottomRightRadius = bottomRightRadius
        this.bottomLeftRadius = bottomLeftRadius

        refresh(true)
    }

    private fun generatePath(useBezier: Boolean, rect: RectF, topLeftRadius: Float, topRightRadius: Float, bottomRightRadius: Float, bottomLeftRadius: Float): Path {
        val path = Path()

        val left = rect.left
        val top = rect.top
        val bottom = rect.bottom
        val right = rect.right

        val maxSize = (rect.width() / 2f).coerceAtMost(rect.height() / 2f)

        var topLeftRadiusAbs = abs(topLeftRadius)
        var topRightRadiusAbs = abs(topRightRadius)
        var bottomLeftRadiusAbs = abs(bottomLeftRadius)
        var bottomRightRadiusAbs = abs(bottomRightRadius)

        if (topLeftRadiusAbs > maxSize) {
            topLeftRadiusAbs = maxSize
        }
        if (topRightRadiusAbs > maxSize) {
            topRightRadiusAbs = maxSize
        }
        if (bottomLeftRadiusAbs > maxSize) {
            bottomLeftRadiusAbs = maxSize
        }
        if (bottomRightRadiusAbs > maxSize) {
            bottomRightRadiusAbs = maxSize
        }

        path.moveTo(left + topLeftRadiusAbs, top)
        path.lineTo(right - topRightRadiusAbs, top)

        if (useBezier) {
            path.quadTo(right, top, right, top + topRightRadiusAbs)
        } else {
            val arc: Float = if (topRightRadius > 0) 90f else -270f
            path.arcTo(RectF(right - topRightRadiusAbs * 2f, top, right, top + topRightRadiusAbs * 2f), -90f, arc)
        }

        path.lineTo(right, bottom - bottomRightRadiusAbs)

        if (useBezier) {
            path.quadTo(right, bottom, right - bottomRightRadiusAbs, bottom)
        } else {
            val arc: Float = if (bottomRightRadiusAbs > 0) 90f else -270f
            path.arcTo(RectF(right - bottomRightRadiusAbs * 2f, bottom - bottomRightRadiusAbs * 2f, right, bottom), 0f, arc)
        }

        path.lineTo(left + bottomLeftRadiusAbs, bottom)

        if (useBezier) {
            path.quadTo(left, bottom, left, bottom - bottomLeftRadiusAbs)
        } else {
            val arc: Float = if (bottomLeftRadiusAbs > 0) 90f else -270f
            path.arcTo(RectF(left, bottom - bottomLeftRadiusAbs * 2f, left + bottomLeftRadiusAbs * 2f, bottom), 90f, arc)
        }

        path.lineTo(left, top + topLeftRadiusAbs)

        if (useBezier) {
            path.quadTo(left, top, left + topLeftRadiusAbs, top)
        } else {
            val arc: Float = if (topLeftRadiusAbs > 0) 90f else -270f
            path.arcTo(RectF(left, top, left + topLeftRadiusAbs * 2f, top + topLeftRadiusAbs * 2f), 180f, arc)
        }

        path.close()

        return path
    }
}