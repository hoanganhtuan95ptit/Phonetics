package com.simple.feature.pronunciation_assessment.domain.repositories

import com.simple.feature.pronunciation_assessment.data.recognizer.Wav2Vec2PhonemeRecognizer
import com.simple.feature.pronunciation_assessment.domain.entities.RecognitionResult
import com.simple.phonetics.PhoneticsApp

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
    fun recognize(audioFloat: FloatArray): List<String> =
        recognizeWithTiming(audioFloat).phonemes

    /**
     * Chạy inference + trả về cả frame timing cho mỗi phoneme.
     *
     * Cần thiết cho chấm điểm vowel length (long/short vowel) — chỉ
     * có duration thực tế mới phân biệt được /iː/ vs /ɪ/ khi model
     * không output dấu kéo dài.
     */
    fun recognizeWithTiming(audioFloat: FloatArray): RecognitionResult

    companion object{

        val instance by lazy {
            Wav2Vec2PhonemeRecognizer(PhoneticsApp.share)
        }
    }
}
