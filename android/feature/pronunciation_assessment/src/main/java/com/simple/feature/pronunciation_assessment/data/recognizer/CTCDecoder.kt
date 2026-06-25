package com.simple.feature.pronunciation_assessment.data.recognizer

/**
 * CTCDecoder — chuyển logits → IPA phoneme list.
 *
 * CTC Greedy Decode:
 *   1. Với mỗi time step, lấy argmax → token có xác suất cao nhất
 *   2. Collapse consecutive duplicates: [k,k,k,æ,æ,t] → [k,æ,t]
 *   3. Bỏ `<pad>` (blank) và các special token khác
 *
 * Model wav2vec2-lv-60-espeak-cv-ft không có token "|" (word delimiter);
 * xuất phoneme phẳng — tách từ làm ở tầng scorer dựa trên alignment.
 */
internal object CTCDecoder {

    /**
     * @param logits     Float32 array shape [time_steps × vocab_size]
     * @param vocabSize  kích thước vocab (392 cho model này)
     * @param vocab      bảng decode id → IPA token
     */
    fun greedyDecode(
        logits: FloatArray,
        vocabSize: Int,
        vocab: Wav2Vec2Vocab,
    ): List<String> {
        val timeSteps = logits.size / vocabSize
        val tokens = ArrayList<String>(timeSteps / 2)
        var prevId = -1

        for (t in 0 until timeSteps) {
            // Argmax trên vocab dimension
            val base = t * vocabSize
            var maxId = 0
            var maxVal = logits[base]
            for (v in 1 until vocabSize) {
                val cur = logits[base + v]
                if (cur > maxVal) {
                    maxVal = cur
                    maxId = v
                }
            }

            // Collapse duplicates + bỏ special tokens
            if (maxId != prevId && !vocab.isSpecial(maxId)) {
                vocab.decode(maxId)?.let { tokens += it }
            }
            prevId = maxId
        }
        return tokens
    }
}
