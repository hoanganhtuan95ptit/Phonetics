package com.simple.phonetics.utils.transformation

import android.graphics.*
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

class ColorFilterTransformation(
    private val color: Int,
    private val alpha: Float // 0f to 1f
) : BitmapTransformation() {

    companion object {
        private const val ID = "com.example.ColorFilterTransformation"
        private val ID_BYTES = ID.toByteArray(Charsets.UTF_8)
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val result = pool.get(toTransform.width, toTransform.height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Vẽ ảnh gốc
        canvas.drawBitmap(toTransform, 0f, 0f, paint)

        // Áp màu phủ
        paint.color = color
        paint.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        canvas.drawRect(0f, 0f, toTransform.width.toFloat(), toTransform.height.toFloat(), paint)

        return result
    }

    override fun equals(other: Any?): Boolean {
        return other is ColorFilterTransformation &&
                other.color == color &&
                other.alpha == alpha
    }

    override fun hashCode(): Int {
        return ID.hashCode() + color * 31 + (alpha * 1000).toInt()
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("$ID-$color-$alpha".toByteArray(Charsets.UTF_8))
    }
}
