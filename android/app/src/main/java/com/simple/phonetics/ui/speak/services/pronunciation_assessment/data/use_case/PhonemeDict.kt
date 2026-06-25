package com.simple.phonetics.ui.speak.services.pronunciation_assessment.data.use_case

import android.content.Context
import org.json.JSONObject

/**
 * Grapheme–phoneme dictionary cho 5000 từ tiếng Anh phổ biến.
 *
 * Mỗi từ là danh sách các cặp (chữ cái, âm IPA):
 *   "the"   → [("th","ð"), ("e","ə")]
 *   "knife" → [("kn","n"), ("i","aɪ"), ("fe","f")]
 *
 * Cách dùng:
 *   1. Đặt file phoneme_dict.gz vào thư mục `assets/`.
 *   2. Khởi tạo:  val dict = PhonemeDict.load(context)
 *   3. Tra cứu :  val pairs = dict["beautiful"]
 *   4. Highlight chữ sai bằng cách so sánh phoneme expected ↔ actual ở
 *      lớp xử lý kết quả phát âm.
 */
data class PhonemeChunk(val grapheme: String, val phoneme: String)

class PhonemeDict private constructor(
    private val data: Map<String, List<PhonemeChunk>>,
) {
    operator fun get(word: String): List<PhonemeChunk>? =
        data[word.lowercase()]

    fun contains(word: String): Boolean = data.containsKey(word.lowercase())

    val size: Int get() = data.size

    /**
     * Cho 1 từ + danh sách phoneme bị lỗi, trả về vị trí ký tự sai.
     *
     * @param word từ gốc (ví dụ "the")
     * @param errorPhonemes danh sách phoneme bị sai (ví dụ ["ð", "ə"])
     * @return list<IntRange> các đoạn ký tự cần highlight đỏ
     */
    fun findErrorRanges(word: String, errorPhonemes: Collection<String>): List<IntRange> {
        val pairs = this[word] ?: return emptyList()
        val ranges = mutableListOf<IntRange>()
        var pos = 0
        for ((grapheme, phoneme) in pairs) {
            val end = pos + grapheme.length
            // Phoneme of chunk có thể là chuỗi nhiều âm (vd "ju", "əl")
            // → check xem bất kỳ phoneme sai nào nằm trong chunk này
            if (errorPhonemes.any { it in phoneme }) {
                ranges += pos until end
            }
            pos = end
        }
        return ranges
    }

    /**
     * Trả về list các cụm chữ cái (grapheme) cần tô màu lỗi.
     * Tiện cho API .with(substring, color) — chỉ cần loop qua list này.
     *
     * Nếu từ không có trong dictionary → trả về [word] (tô màu cả từ).
     */
    fun findErrorGraphemes(word: String, errorPhonemes: Collection<String>): List<String> {
        if (errorPhonemes.isEmpty()) return emptyList()
        val pairs = this[word] ?: return listOf(word)
        return pairs
            .filter { (_, phoneme) -> errorPhonemes.any { it in phoneme } }
            .map { it.grapheme }
            .distinct()
    }

    /**
     * Map từng cụm chữ (grapheme) → điểm phoneme tương ứng (lấy min nếu chunk
     * chứa nhiều phoneme).
     *
     * Dùng để tô màu từng cụm chữ theo điểm GOP (red/yellow/blue).
     *
     * @param word từ gốc
     * @param phonemeScores điểm theo thứ tự phoneme (đã loại INSERTION)
     * @return list (grapheme, score) theo thứ tự xuất hiện trong từ.
     *         Nếu từ không có trong dict → trả về [(word, min(scores))].
     */
    fun findGraphemeScores(
        word: String,
        phonemeScores: List<Int>,
    ): List<Pair<String, Int>> {
        val pairs = this[word] ?: return listOf(word to (phonemeScores.minOrNull() ?: 0))
        val result = ArrayList<Pair<String, Int>>(pairs.size)
        var idx = 0
        for ((grapheme, phonemeStr) in pairs) {
            val tokenCount = tokenizePhonemes(phonemeStr).size
            var chunkScore = Int.MAX_VALUE
            repeat(tokenCount) {
                val s = phonemeScores.getOrNull(idx)
                if (s != null && s < chunkScore) chunkScore = s
                idx++
            }
            result += grapheme to (if (chunkScore == Int.MAX_VALUE) 0 else chunkScore)
        }
        return result
    }

    companion object {
        // Tập IPA phoneme dùng trong dictionary — 2-char trước để greedy match dài nhất.
        private val ATOMIC_PHONEMES = listOf(
            "tʃ", "dʒ", "aɪ", "aʊ", "eɪ", "oʊ", "ɔɪ",
            "ð", "θ", "ŋ", "ʃ", "ʒ",
            "æ", "ɑ", "ɔ", "ə", "ɛ", "ɪ", "ʊ", "ʌ", "ɚ", "ɝ", "ɡ", "ɹ",
            "b", "d", "f", "g", "h", "i", "j", "k", "l", "m",
            "n", "p", "r", "s", "t", "u", "v", "w", "z",
        )

        /** Tách phoneme_str (vd "ju", "ks", "tʃ") thành list phoneme đơn. */
        fun tokenizePhonemes(s: String): List<String> {
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
         * Đọc dictionary từ assets/phoneme_dict.gz.
         * Mở 1 lần khi app khởi động, lưu trong Singleton.
         */
        fun load(context: Context): PhonemeDict {
            val jsonStr = context.assets.open("phoneme_dict.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(jsonStr)
            val map = HashMap<String, List<PhonemeChunk>>(root.length())
            val keys = root.keys()
            while (keys.hasNext()) {
                val word = keys.next()
                val arr = root.getJSONArray(word)
                val chunks = ArrayList<PhonemeChunk>(arr.length())
                for (i in 0 until arr.length()) {
                    val pair = arr.getJSONArray(i)
                    chunks += PhonemeChunk(pair.getString(0), pair.getString(1))
                }
                map[word] = chunks
            }
            return PhonemeDict(map)
        }
    }
}
