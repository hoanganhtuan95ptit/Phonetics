package com.simple.feature.pronunciation_assessment.domain.entities

/**
 * Một cặp (grapheme, phoneme) trong dictionary.
 *
 * Ví dụ "knife" → [("kn","n"), ("i","aɪ"), ("fe","f")].
 */
data class PhonemeChunk(
    val grapheme: String,
    val phoneme: String,
)
