package com.simple.feature.pronunciation_assessment.domain.scoring

/**
 * Tách chuỗi phoneme (vd "ju", "ks", "tʃ") thành list phoneme đơn.
 *
 * Dùng greedy matching để ưu tiên token nhiều ký tự trước (tʃ, dʒ, iː, ...).
 */
object PhonemeTokenizer {

    // Tập IPA phoneme dùng trong dictionary — 2-char trước để greedy match dài nhất.
    private val ATOMIC_PHONEMES = listOf(
        "tʃ", "dʒ", "aɪ", "aʊ", "eɪ", "oʊ", "ɔɪ",
        "ð", "θ", "ŋ", "ʃ", "ʒ",
        "æ", "ɑ", "ɔ", "ə", "ɛ", "ɪ", "ʊ", "ʌ", "ɚ", "ɝ", "ɡ", "ɹ",
        "b", "d", "f", "g", "h", "i", "j", "k", "l", "m",
        "n", "p", "r", "s", "t", "u", "v", "w", "z",
    )

    fun tokenize(s: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < s.length) {
            var matched = false
            for (p in ATOMIC_PHONEMES) {
                if (s.startsWith(p, i)) {
                    result += p
                    i += p.length
                    matched = true
                    break
                }
            }
            if (!matched) {
                result += s[i].toString()
                i++
            }
        }
        return result
    }

    /**
     * Tách chuỗi IPA dạng "/ˈtiːm/" thành list phoneme.
     * Bỏ delimiter '/' và stress markers ˈ ˌ trước khi tokenize.
     */
    fun parseIpa(ipa: String): List<String> =
        tokenize(ipa.replace("/", "").replace("ˈ", "").replace("ˌ", "").trim())
}
