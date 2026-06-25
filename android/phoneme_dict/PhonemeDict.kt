package com.ephonetics.phoneme

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.GZIPInputStream

/**
 * Grapheme–phoneme dictionary cho 5000 từ tiếng Anh phổ biến.
 *
 * Mỗi từ là danh sách các cặp (chữ cái, âm IPA):
 *   "the"   → [("th","ð"), ("e","ə")]
 *   "knife" → [("kn","n"), ("i","aɪ"), ("fe","f")]
 *
 * Cách dùng:
 *   1. Đặt file phoneme_dict.json.gz vào thư mục `assets/`.
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
    fun findErrorRanges(word: String, errorPhonemes: List<String>): List<IntRange> {
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

    companion object {
        /**
         * Đọc dictionary từ assets/phoneme_dict.json.gz.
         * Mở 1 lần khi app khởi động, lưu trong Singleton.
         */
        fun load(context: Context): PhonemeDict {
            val input = context.assets.open("phoneme_dict.json.gz")
            val jsonStr = GZIPInputStream(input).bufferedReader(Charsets.UTF_8).use { it.readText() }
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
