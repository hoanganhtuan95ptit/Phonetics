package com.simple.feature.pronunciation_assessment.data.audio

/**
 * Buffer float thuần — auto-grow như ArrayList nhưng KHÔNG box.
 * Dùng cho speechBuffer (16 kHz × 15 s = 240k samples).
 *
 * So với MutableList<Short>: tiết kiệm ~6 MB heap và tránh GC stall.
 */
internal class FloatGrowBuffer(initialCapacity: Int) {

    private var data = FloatArray(initialCapacity)

    var size: Int = 0
        private set

    fun appendShortAsFloat(src: ShortArray, length: Int) {
        ensureCapacity(size + length)
        var i = size
        for (k in 0 until length) {
            data[i++] = src[k] / 32768.0f
        }
        size = i
    }

    fun snapshot(): FloatArray = data.copyOf(size)

    fun clear() {
        size = 0
        // không shrink — giữ capacity cho lần ghi sau
    }

    fun isEmpty(): Boolean = size == 0

    private fun ensureCapacity(min: Int) {
        if (min <= data.size) return
        var newCap = data.size + (data.size shr 1) // grow ×1.5
        if (newCap < min) newCap = min
        data = data.copyOf(newCap)
    }
}
