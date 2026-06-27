package com.simple.feature.pronunciation_assessment.domain.entities

/**
 * Phoneme kèm thông tin frame (CTC alignment) từ wav2vec2.
 *
 * @param phoneme   token IPA mà model decode được (vd "m", "i", "iː")
 * @param startMs   thời điểm bắt đầu phoneme này trong audio (ms)
 * @param endMs     thời điểm kết thúc (ms) — `endMs - startMs` = duration
 *
 * Wav2Vec2 base stride 320 samples @ 16 kHz → 1 frame ≈ 20 ms. Recognizer
 * gộp các frame liên tiếp cùng token → start/end thời điểm thực.
 */
data class PhonemeFrame(
    val phoneme: String,
    val startMs: Int,
    val endMs: Int,
) {
    val durationMs: Int get() = endMs - startMs
}

/**
 * Kết quả inference đầy đủ: phoneme list + frame timing.
 * Dùng cho scorer khi cần chấm điểm vowel length.
 */
data class RecognitionResult(
    val phonemes: List<String>,
    val frames: List<PhonemeFrame>,
)
