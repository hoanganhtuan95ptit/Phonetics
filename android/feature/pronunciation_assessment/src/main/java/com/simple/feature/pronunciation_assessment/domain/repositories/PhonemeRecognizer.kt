package com.simple.feature.pronunciation_assessment.domain.repositories

/**
 * Nhận dạng phoneme từ audio float32 (16 kHz mono, normalized).
 *
 * Implementation hiện tại dùng wav2vec2-lv-60-espeak-cv-ft chạy qua
 * OnnxRuntime Android.
 */
interface PhonemeRecognizer : AutoCloseable {

    /**
     * Tải model + vocab. Blocking I/O — gọi trên background thread.
     *
     * @param useGPU     dùng NNAPI nếu có
     * @param onProgress callback tiến trình tải model (0–100). Gọi trên
     *                   thread hiện tại. Báo 100 ngay nếu file đã cache.
     */
    suspend fun load(useGPU: Boolean = false, onProgress: ((percent: Int) -> Unit)? = null)

    /**
     * Chạy inference trên 1 chunk audio.
     *
     * @param audioFloat PCM float32 [-1, 1], 16 kHz, mono, đã normalize
     *                   zero-mean / unit-variance.
     * @return list IPA phonemes, ví dụ ["h", "ɛ", "l", "oʊ"].
     */
    fun recognize(audioFloat: FloatArray): List<String>
}
