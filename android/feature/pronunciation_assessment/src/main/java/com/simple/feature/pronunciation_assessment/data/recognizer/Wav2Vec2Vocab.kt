package com.simple.feature.pronunciation_assessment.data.recognizer

import android.content.Context
import org.json.JSONObject

/**
 * Vocabulary của Wav2Vec2 tokenizer — load lúc init, không hardcode.
 *
 * Format file vocab.json (do tools/export_wav2vec2_onnx.py sinh):
 * ```
 *   {
 *     "id_to_token": ["<pad>","<s>","</s>","<unk>","n","s","t",...,"a4"],
 *     "pad_id":      0,
 *     "special_ids": [0, 1, 2, 3],
 *     "word_delimiter_id": null
 *   }
 * ```
 *
 * QUAN TRỌNG: model wav2vec2-lv-60-espeak-cv-ft có 392 token thứ tự
 * CỤ THỂ. Hardcode tay → sai 1 index = decode ra IPA hoàn toàn khác,
 * scorer thấy substitution liên tục, điểm chìm về 10/100.
 */
internal class Wav2Vec2Vocab(
    val idToToken: Array<String>,
    val padId: Int,
    val specialIds: IntArray,
    val wordDelimiterId: Int, // -1 nếu model không có
) {

    val size: Int get() = idToToken.size

    private val specialSet: Set<Int> = specialIds.toHashSet()

    /** True nếu id là special (`<pad>`/`<s>`/`</s>`/`<unk>`) hoặc word delimiter. */
    fun isSpecial(id: Int): Boolean =
        id in specialSet || (wordDelimiterId >= 0 && id == wordDelimiterId)

    fun decode(id: Int): String? = idToToken.getOrNull(id)

    companion object {
        const val ASSET_FILE_NAME = "wav2vec2_phoneme_vocab.json"

        fun loadFromAssets(context: Context, fileName: String = ASSET_FILE_NAME): Wav2Vec2Vocab {
            val json = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val root = JSONObject(json)

            val arr = root.getJSONArray("id_to_token")
            val tokens = Array(arr.length()) { arr.getString(it) }

            val padId = root.optInt("pad_id", 0)

            val specialArr = root.optJSONArray("special_ids")
            val specials = IntArray(specialArr?.length() ?: 0) { specialArr!!.getInt(it) }

            val wdt = if (root.isNull("word_delimiter_id")) -1
            else root.optInt("word_delimiter_id", -1)

            return Wav2Vec2Vocab(
                idToToken = tokens,
                padId = padId,
                specialIds = specials,
                wordDelimiterId = wdt,
            )
        }
    }
}
