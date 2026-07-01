package com.simple.feature.pronunciation_assessment.domain.scoring

import com.simple.feature.pronunciation_assessment.domain.entities.PhonemeFrame
import com.simple.feature.pronunciation_assessment.domain.scoring.VowelLengthScorer.averageVowelDurationMs

/**
 * VowelLengthScorer — chấm điểm độ dài nguyên âm dựa trên duration thực tế.
 *
 * Vì wav2vec2 thường KHÔNG output dấu kéo dài `ː`, ta không thể dựa vào
 * token để biết người dùng phát âm long hay short. Cách duy nhất là đo
 * **duration của vowel trong audio** rồi so với trung bình vowel trong
 * cùng câu (normalize theo tốc độ nói của speaker).
 *
 * Thuật toán:
 *   1. Tính `avgVowelMs` = trung bình duration của các vowel trong câu.
 *   2. Với mỗi vowel cần chấm:
 *        ratio = duration / avgVowelMs
 *      - expected long (`isLong = true`):
 *          ratio < 0.9 → TOO_SHORT (penalty mạnh)
 *      - expected short (`isLong = false`):
 *          ratio > 1.6 → TOO_LONG  (penalty nhẹ — ít quan trọng hơn)
 *      - còn lại → OK, không penalty
 *
 * Tại sao dùng ratio thay vì ms cố định?
 *   - Tốc độ nói khác nhau giữa người và giữa câu. Ms cố định (vd 120ms)
 *     sai khi người nói nhanh (vowel dài thật cũng < 120ms) hoặc chậm
 *     (vowel ngắn cũng > 120ms).
 *   - Ratio so với trung bình trong cùng câu là invariant với tempo.
 */
internal object VowelLengthScorer {

    /** Threshold ratio so với avg vowel duration (mode chính, câu nhiều vowel). */
    private const val LONG_MIN_RATIO = 0.9f   // long vowel phải ≥ 0.9× avg
    private const val SHORT_MAX_RATIO = 1.6f  // short vowel không quá 1.6× avg

    /**
     * Fallback absolute thresholds (ms) — chỉ dùng khi câu có < 2 vowel để
     * có baseline ratio. Số dựa trên trung bình tiếng Anh (Ladefoged):
     *   - short vowel: ~70–90 ms
     *   - long vowel:  ~150–180 ms
     */
    private const val LONG_MIN_MS = 130
    private const val SHORT_MAX_MS = 110

    /** Penalty trừ trực tiếp vào điểm GOP (0–100). */
    private const val PENALTY_TOO_SHORT = 30  // nói long thành short — sai rõ
    private const val PENALTY_TOO_LONG = 12   // nói short thành long — ít gây nhầm nghĩa

    /** Số tối thiểu vowel để baseline đáng tin (tránh chia khi câu ngắn). */
    private const val MIN_VOWELS_FOR_BASELINE = 2

    enum class LengthVerdict { OK, TOO_SHORT, TOO_LONG, UNKNOWN }

    data class LengthResult(
        val verdict: LengthVerdict,
        val penalty: Int,
    )

    /**
     * Tính duration trung bình của các vowel trong frame list.
     * Trả về 0 nếu không đủ data — caller phải check và bỏ qua length scoring.
     */
    fun averageVowelDurationMs(frames: List<PhonemeFrame>): Float {
        val vowelDurs = frames.filter { isVowel(it.phoneme) }.map { it.durationMs }
        if (vowelDurs.size < MIN_VOWELS_FOR_BASELINE) return 0f
        return vowelDurs.sum().toFloat() / vowelDurs.size
    }

    /**
     * Chấm length cho 1 vowel.
     *
     * @param expectedLong  reference đánh dấu phoneme này là long (`ː`)?
     * @param actualMs      duration thực tế của phoneme được align từ hyp
     * @param avgVowelMs    avg vowel duration của câu (từ [averageVowelDurationMs])
     */
    fun evaluate(
        expectedLong: Boolean,
        actualMs: Int,
        avgVowelMs: Float,
    ): LengthResult {
        if (actualMs <= 0) return LengthResult(LengthVerdict.UNKNOWN, penalty = 0)

        // Mode 1: dùng ratio so với avg vowel của câu (chính xác hơn, invariant tempo).
        if (avgVowelMs > 0f) {
            val ratio = actualMs / avgVowelMs
            return when {
                expectedLong && ratio < LONG_MIN_RATIO ->
                    LengthResult(LengthVerdict.TOO_SHORT, PENALTY_TOO_SHORT)
                !expectedLong && ratio > SHORT_MAX_RATIO ->
                    LengthResult(LengthVerdict.TOO_LONG, PENALTY_TOO_LONG)
                else ->
                    LengthResult(LengthVerdict.OK, 0)
            }
        }

        // Mode 2 (fallback): câu chỉ có 1 vowel → dùng ngưỡng ms tuyệt đối.
        return when {
            expectedLong && actualMs < LONG_MIN_MS ->
                LengthResult(LengthVerdict.TOO_SHORT, PENALTY_TOO_SHORT)
            !expectedLong && actualMs > SHORT_MAX_MS ->
                LengthResult(LengthVerdict.TOO_LONG, PENALTY_TOO_LONG)
            else ->
                LengthResult(LengthVerdict.OK, 0)
        }
    }

    /**
     * Vowel check — dựa trên ký tự đầu (đã strip ː). Bao phủ IPA tiếng Anh.
     * Public để scorer biết có nên áp length penalty hay không
     * (consonant không có khái niệm long/short).
     */
    fun isVowel(phoneme: String): Boolean {
        if (phoneme.isEmpty()) return false
        val base = phoneme.removeSuffix("ː").trimStart('ˈ', 'ˌ')
        return base in VOWEL_BASES || base.firstOrNull() in VOWEL_FIRST_CHARS
    }

    private val VOWEL_BASES = setOf(
        "i", "ɪ", "e", "ɛ", "æ", "a", "ɑ", "ɒ", "ʌ", "ə", "ɜ", "ɝ", "ɚ",
        "ɔ", "o", "ʊ", "u",
        "eɪ", "aɪ", "ɔɪ", "aʊ", "oʊ", "ɪə", "eə", "ʊə",
    )

    private val VOWEL_FIRST_CHARS = setOf('i', 'ɪ', 'e', 'ɛ', 'æ', 'a', 'ɑ', 'ɒ',
        'ʌ', 'ə', 'ɜ', 'ɝ', 'ɚ', 'ɔ', 'o', 'ʊ', 'u')
}
