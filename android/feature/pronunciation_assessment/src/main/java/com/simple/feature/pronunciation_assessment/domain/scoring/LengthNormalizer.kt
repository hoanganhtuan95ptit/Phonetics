package com.simple.feature.pronunciation_assessment.domain.scoring

/**
 * LengthNormalizer — chuẩn hoá phoneme list bằng cách **tách dấu kéo dài (ː)
 * khỏi sequence**, biến nó thành metadata `isLong` của nguyên âm đứng trước.
 *
 * Lý do:
 *   - Reference từ dictionary thường có `ː` như 1 token riêng:
 *       "meet" → ["m", "i", "ː", "t"]
 *   - Wav2Vec2 (wav2vec2-lv-60-espeak-cv-ft) lại có 3 hành vi tùy model:
 *       (a) Strip ː hoàn toàn        → ["m", "i", "t"]
 *       (b) Gộp thành 1 token        → ["m", "iː", "t"]
 *       (c) Tách ː riêng (hiếm)      → ["m", "i", "ː", "t"]
 *   - Nếu so khớp 1-1 thẳng → false SUBSTITUTION liên tục.
 *
 * Sau khi normalize:
 *   - "meet" → [Norm("m",false), Norm("i",true), Norm("t",false)]
 *   - hyp "[m,i,t]" → [Norm("m",false), Norm("i",false), Norm("t",false)]
 *   - Aligner so trên trường `base` → tất cả CORRECT.
 *   - Độ dài (`isLong`) được scorer chấm riêng dựa trên duration thực tế của
 *     phoneme trong audio (xem [PronunciationScorer]).
 */
internal object LengthNormalizer {

    private const val LENGTH_MARK = "ː"

    /** Phoneme đã loại bỏ dấu kéo dài, kèm flag long/short. */
    data class Norm(
        val base: String,
        val isLong: Boolean,
    )

    /**
     * Normalize 1 sequence phoneme bất kỳ (ref hoặc hyp).
     *
     * Rules:
     *  - Token trùng `"ː"` → gán `isLong = true` cho phần tử cuối cùng đã add,
     *    không add token này vào output.
     *  - Token có hậu tố `"ː"` (vd `"iː"`, `"uː"`) → tách thành `(base, true)`.
     *  - Mọi token khác → `(token, false)`.
     */
    fun normalize(phonemes: List<String>): List<Norm> {
        val out = ArrayList<Norm>(phonemes.size)
        for (p in phonemes) {
            when {
                p == LENGTH_MARK -> {
                    // Dấu kéo dài đứng riêng → upgrade phần tử cuối thành long
                    val last = out.lastOrNull() ?: continue
                    out[out.lastIndex] = last.copy(isLong = true)
                }
                p.endsWith(LENGTH_MARK) && p.length > 1 -> {
                    out += Norm(base = p.removeSuffix(LENGTH_MARK), isLong = true)
                }
                else -> {
                    out += Norm(base = p, isLong = false)
                }
            }
        }
        return out
    }

    /** Tiện lợi — chỉ lấy danh sách `base` cho aligner. */
    fun bases(norms: List<Norm>): List<String> = norms.map { it.base }
}
