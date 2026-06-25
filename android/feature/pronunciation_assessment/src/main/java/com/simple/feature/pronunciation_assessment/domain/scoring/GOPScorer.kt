package com.simple.feature.pronunciation_assessment.domain.scoring

import com.simple.feature.pronunciation_assessment.domain.entities.ConsonantFeatures
import com.simple.feature.pronunciation_assessment.domain.entities.Manner
import com.simple.feature.pronunciation_assessment.domain.entities.Place
import com.simple.feature.pronunciation_assessment.domain.entities.VowelBackness
import com.simple.feature.pronunciation_assessment.domain.entities.VowelFeatures
import com.simple.feature.pronunciation_assessment.domain.entities.VowelHeight
import com.simple.phonetics.entities.ErrorType
import kotlin.math.abs

/**
 * GOPScorer — tính confusion cost và điểm GOP (Goodness of Pronunciation)
 * cho từng phoneme.
 *
 * Cost được tính từ phonetic feature distance, không hardcode theo quốc
 * tịch người dùng. Mọi ngôn ngữ mẹ đẻ đều dùng cùng một formula — cost tự
 * nhiên cao hơn khi 2 âm khác nhau nhiều về acoustic properties.
 */
object GOPScorer {

    /**
     * Tính confusion cost (0–95) giữa 2 phoneme.
     *
     * - Consonant: voicing + manner distance + place distance
     * - Vowel:     height + backness + rounding + tenseness + diphthong
     * - Cross-class (consonant vs vowel) hoặc phoneme lạ: 65
     */
    fun confusionCost(expected: String, actual: String): Int {
        if (expected == actual) return 0
        val c1 = CONSONANT_TABLE[expected]
        val c2 = CONSONANT_TABLE[actual]
        val v1 = VOWEL_TABLE[expected]
        val v2 = VOWEL_TABLE[actual]
        return when {
            c1 != null && c2 != null -> consonantCost(c1, c2)
            v1 != null && v2 != null -> vowelCost(v1, v2)
            // Cross-class hoặc phoneme lạ — cost 65 thay vì 90 để tránh
            // "vực 10/100" khi vocab decode ra phoneme tiếng nước ngoài.
            else -> 65
        }
    }

    /**
     * Tính điểm GOP 0–100 cho một phoneme.
     *  - Đúng (expected == actual) → 90 + baseNoise, capped [82, 100]
     *  - Deletion (actual == null)  → 0
     *  - Sai (substitution)         → 100 − confusionCost
     */
    fun score(expected: String, actual: String?, baseNoise: Int = 0): Int {
        if (actual == null) return 0
        if (expected == actual) return (90 + baseNoise).coerceIn(82, 100)
        return (100 - confusionCost(expected, actual)).coerceIn(0, 100)
    }

    fun errorType(expected: String, actual: String?): ErrorType = when {
        actual == null -> ErrorType.DELETION
        expected == actual -> ErrorType.CORRECT
        else -> ErrorType.SUBSTITUTION
    }
}

// ─────────────────────────────────────────────
// Distance helpers — internal
// ─────────────────────────────────────────────

/**
 * Khoảng cách giữa 2 [Manner] (0 = giống, 4 = rất khác).
 * Symmetric — chỉ lưu (min, max) index.
 */
private val MANNER_DIST: Map<Pair<Int, Int>, Int> = buildMap {
    //            PLOS FRIC AFFC NASL APRX LATL RHOT GLID
    val raw = listOf(
        Pair(0, 0) to 0, Pair(0, 1) to 3, Pair(0, 2) to 2, Pair(0, 3) to 3,
        Pair(0, 4) to 4, Pair(0, 5) to 4, Pair(0, 6) to 4, Pair(0, 7) to 4,
        Pair(1, 1) to 0, Pair(1, 2) to 2, Pair(1, 3) to 4, Pair(1, 4) to 3,
        Pair(1, 5) to 3, Pair(1, 6) to 3, Pair(1, 7) to 3,
        Pair(2, 2) to 0, Pair(2, 3) to 4, Pair(2, 4) to 4, Pair(2, 5) to 4,
        Pair(2, 6) to 4, Pair(2, 7) to 4,
        Pair(3, 3) to 0, Pair(3, 4) to 2, Pair(3, 5) to 2, Pair(3, 6) to 3, Pair(3, 7) to 3,
        Pair(4, 4) to 0, Pair(4, 5) to 1, Pair(4, 6) to 1, Pair(4, 7) to 1,
        Pair(5, 5) to 0, Pair(5, 6) to 1, Pair(5, 7) to 2,
        Pair(6, 6) to 0, Pair(6, 7) to 2,
        Pair(7, 7) to 0,
    )
    raw.forEach { (k, v) ->
        put(k, v)
        put(Pair(k.second, k.first), v) // symmetric
    }
}

private fun mannerDist(a: Manner, b: Manner): Int =
    MANNER_DIST[Pair(a.ordinal, b.ordinal)] ?: 5

private fun placeDist(a: Place, b: Place): Int =
    abs(a.ordinal - b.ordinal)

/**
 * Tính confusion cost giữa 2 consonant.
 *
 * Weights:
 *   voicing     = 15   (dễ nhầm, ít ảnh hưởng nhất)
 *   manner_step = 10
 *   place_step  = 7
 *
 * Ví dụ:
 *   /p/→/b/ = voicing only           → 15  (score 85)
 *   /p/→/t/ = same manner, 3 place   → 21  (score 79)
 *   /θ/→/t/ = differ manner + place  → 37  (score 63)
 */
private fun consonantCost(f1: ConsonantFeatures, f2: ConsonantFeatures): Int {
    val voicing = if (f1.voiced == f2.voiced) 0 else 15
    val manner = mannerDist(f1.manner, f2.manner) * 10
    val place = placeDist(f1.place, f2.place) * 7
    return (voicing + manner + place).coerceAtMost(95)
}

/**
 * Tính confusion cost giữa 2 vowel.
 *
 * Weights:
 *   height_step   = 18  (quan trọng nhất với nguyên âm)
 *   backness_step = 14
 *   rounded       = 10
 *   tense         = 8
 *   diphthong     = 14
 */
private fun vowelCost(v1: VowelFeatures, v2: VowelFeatures): Int {
    val height = abs(v1.height.level - v2.height.level) * 18
    val backness = abs(v1.backness.level - v2.backness.level) * 14
    val rounded = if (v1.rounded == v2.rounded) 0 else 10
    val tense = if (v1.tense == v2.tense) 0 else 8
    val diph = if (v1.diphthong == v2.diphthong) 0 else 14
    return (height + backness + rounded + tense + diph).coerceAtMost(90)
}

// ─────────────────────────────────────────────
// IPA phoneme tables (English)
// ─────────────────────────────────────────────

private val CONSONANT_TABLE: Map<String, ConsonantFeatures> = mapOf(
    // Plosives
    "p" to ConsonantFeatures(Manner.PLOSIVE, Place.BILABIAL, voiced = false),
    "b" to ConsonantFeatures(Manner.PLOSIVE, Place.BILABIAL, voiced = true),
    "t" to ConsonantFeatures(Manner.PLOSIVE, Place.ALVEOLAR, voiced = false),
    "d" to ConsonantFeatures(Manner.PLOSIVE, Place.ALVEOLAR, voiced = true),
    "k" to ConsonantFeatures(Manner.PLOSIVE, Place.VELAR, voiced = false),
    "ɡ" to ConsonantFeatures(Manner.PLOSIVE, Place.VELAR, voiced = true),
    // Fricatives
    "f" to ConsonantFeatures(Manner.FRICATIVE, Place.LABIODENTAL, voiced = false),
    "v" to ConsonantFeatures(Manner.FRICATIVE, Place.LABIODENTAL, voiced = true),
    "θ" to ConsonantFeatures(Manner.FRICATIVE, Place.DENTAL, voiced = false),
    "ð" to ConsonantFeatures(Manner.FRICATIVE, Place.DENTAL, voiced = true),
    "s" to ConsonantFeatures(Manner.FRICATIVE, Place.ALVEOLAR, voiced = false),
    "z" to ConsonantFeatures(Manner.FRICATIVE, Place.ALVEOLAR, voiced = true),
    "ʃ" to ConsonantFeatures(Manner.FRICATIVE, Place.POSTALVEOLAR, voiced = false),
    "ʒ" to ConsonantFeatures(Manner.FRICATIVE, Place.POSTALVEOLAR, voiced = true),
    "h" to ConsonantFeatures(Manner.FRICATIVE, Place.GLOTTAL, voiced = false),
    // Affricates
    "tʃ" to ConsonantFeatures(Manner.AFFRICATE, Place.POSTALVEOLAR, voiced = false),
    "dʒ" to ConsonantFeatures(Manner.AFFRICATE, Place.POSTALVEOLAR, voiced = true),
    // Nasals
    "m" to ConsonantFeatures(Manner.NASAL, Place.BILABIAL, voiced = true),
    "n" to ConsonantFeatures(Manner.NASAL, Place.ALVEOLAR, voiced = true),
    "ŋ" to ConsonantFeatures(Manner.NASAL, Place.VELAR, voiced = true),
    // Lateral / Rhotic / Glide
    "l" to ConsonantFeatures(Manner.LATERAL, Place.ALVEOLAR, voiced = true),
    "r" to ConsonantFeatures(Manner.RHOTIC, Place.ALVEOLAR, voiced = true),
    "w" to ConsonantFeatures(Manner.GLIDE, Place.BILABIAL, voiced = true),
    "j" to ConsonantFeatures(Manner.GLIDE, Place.PALATAL, voiced = true),
)

private val VOWEL_TABLE: Map<String, VowelFeatures> = mapOf(
    // High front
    "iː" to VowelFeatures(VowelHeight.HIGH, VowelBackness.FRONT, rounded = false, tense = true),
    "ɪ" to VowelFeatures(VowelHeight.HIGH, VowelBackness.FRONT, rounded = false, tense = false),
    // High back
    "uː" to VowelFeatures(VowelHeight.HIGH, VowelBackness.BACK, rounded = true, tense = true),
    "ʊ" to VowelFeatures(VowelHeight.HIGH, VowelBackness.BACK, rounded = true, tense = false),
    // Mid front
    "eɪ" to VowelFeatures(VowelHeight.MID, VowelBackness.FRONT, rounded = false, tense = true, diphthong = true),
    "e" to VowelFeatures(VowelHeight.MID, VowelBackness.FRONT, rounded = false, tense = true),
    "ɛ" to VowelFeatures(VowelHeight.MID, VowelBackness.FRONT, rounded = false, tense = false),
    // Mid central
    "ə" to VowelFeatures(VowelHeight.MID, VowelBackness.CENTRAL, rounded = false, tense = false),
    "ɜː" to VowelFeatures(VowelHeight.MID, VowelBackness.CENTRAL, rounded = false, tense = true),
    "ʌ" to VowelFeatures(VowelHeight.MID, VowelBackness.CENTRAL, rounded = false, tense = false),
    // Mid back
    "oʊ" to VowelFeatures(VowelHeight.MID, VowelBackness.BACK, rounded = true, tense = true, diphthong = true),
    "ɔː" to VowelFeatures(VowelHeight.MID, VowelBackness.BACK, rounded = true, tense = true),
    // Low front
    "æ" to VowelFeatures(VowelHeight.LOW, VowelBackness.FRONT, rounded = false, tense = false),
    // Low central / back
    "a" to VowelFeatures(VowelHeight.LOW, VowelBackness.CENTRAL, rounded = false, tense = false),
    "ɑː" to VowelFeatures(VowelHeight.LOW, VowelBackness.BACK, rounded = false, tense = true),
    // Diphthongs
    "aɪ" to VowelFeatures(VowelHeight.LOW, VowelBackness.FRONT, rounded = false, tense = true, diphthong = true),
    "aʊ" to VowelFeatures(VowelHeight.LOW, VowelBackness.BACK, rounded = false, tense = true, diphthong = true),
    "ɔɪ" to VowelFeatures(VowelHeight.MID, VowelBackness.BACK, rounded = true, tense = true, diphthong = true),
)
