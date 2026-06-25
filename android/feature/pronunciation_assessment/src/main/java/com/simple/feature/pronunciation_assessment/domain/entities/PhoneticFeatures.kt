package com.simple.feature.pronunciation_assessment.domain.entities

/**
 * Đặc trưng âm vị học — dùng để tính khoảng cách acoustic giữa 2 phoneme
 * trong [com.simple.feature.pronunciation_assessment.domain.scoring.GOPScorer].
 */

// ── Consonant ─────────────────────────────────

enum class Manner {
    PLOSIVE, FRICATIVE, AFFRICATE, NASAL, APPROXIMANT, LATERAL, RHOTIC, GLIDE,
}

enum class Place {
    BILABIAL, LABIODENTAL, DENTAL, ALVEOLAR, POSTALVEOLAR, PALATAL, VELAR, GLOTTAL,
}

data class ConsonantFeatures(
    val manner: Manner,
    val place: Place,
    val voiced: Boolean,
)

// ── Vowel ─────────────────────────────────────

enum class VowelHeight(val level: Int) { HIGH(3), MID(2), LOW(1) }
enum class VowelBackness(val level: Int) { FRONT(1), CENTRAL(2), BACK(3) }

data class VowelFeatures(
    val height: VowelHeight,
    val backness: VowelBackness,
    val rounded: Boolean,
    val tense: Boolean,
    val diphthong: Boolean = false,
)
