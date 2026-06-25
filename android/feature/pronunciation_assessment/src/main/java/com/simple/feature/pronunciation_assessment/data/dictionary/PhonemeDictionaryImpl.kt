package com.simple.feature.pronunciation_assessment.data.dictionary

import android.content.Context
import com.simple.feature.pronunciation_assessment.domain.entities.PhonemeChunk
import com.simple.feature.pronunciation_assessment.domain.repositories.PhonemeDictionary
import org.json.JSONObject

/**
 * In-memory implementation đọc dictionary từ asset JSON.
 *
 * Format file `phoneme_dict.json`:
 * ```
 *   { "the": [["th","ð"], ["e","ə"]], "knife": [["kn","n"], ...] }
 * ```
 */
class PhonemeDictionaryImpl private constructor(
    private val data: Map<String, List<PhonemeChunk>>,
) : PhonemeDictionary {

    override val size: Int get() = data.size

    override operator fun get(word: String): List<PhonemeChunk>? =
        data[word.lowercase()]

    override fun contains(word: String): Boolean =
        data.containsKey(word.lowercase())

    companion object {

        private const val ASSET_FILE = "phoneme_dict.json"

        /** Đọc dictionary từ `assets/phoneme_dict.json`. Mở 1 lần khi app khởi động. */
        fun load(context: Context, assetFileName: String = ASSET_FILE): PhonemeDictionaryImpl {
            val jsonStr = context.assets
                .open(assetFileName)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

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
            return PhonemeDictionaryImpl(map)
        }
    }
}
