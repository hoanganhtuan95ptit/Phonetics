package com.simple.feature.pronunciation_assessment.domain.repositories

import com.simple.feature.pronunciation_assessment.domain.entities.PhonemeChunk

/**
 * Dictionary grapheme–phoneme cho các từ tiếng Anh phổ biến.
 *
 * Mỗi từ là danh sách các cặp (chữ cái, âm IPA):
 *   "the"   → [("th","ð"), ("e","ə")]
 *   "knife" → [("kn","n"), ("i","aɪ"), ("fe","f")]
 */
interface PhonemeDictionary {

    val size: Int

    operator fun get(word: String): List<PhonemeChunk>?

    fun contains(word: String): Boolean
}
