package com.simple.feature.pronunciation_assessment.data.recognizer

import com.simple.feature.pronunciation_assessment.domain.entities.PhonemeFrame

/**
 * CTCDecoder — chuyển logits → IPA phoneme list (+ optionally frame timing).
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
    ): List<String> = greedyDecodeWithTiming(logits, vocabSize, vocab, frameMs = 0f)
        .map { it.phoneme }

    /**
     * Greedy decode + ước lượng duration cho từng phoneme.
     *
     * ## Vì sao tính duration không phải là "đếm frame liên tiếp"
     *
     * Wav2Vec2 + CTC có hành vi **peaky**: model học cách emit token đúng 1
     * frame rồi phát `<pad>` (blank) ở các frame kế tiếp, kể cả khi âm
     * acoustic thực sự còn kéo dài. Ví dụ vowel /iː/ dài 180ms thật, model
     * vẫn có thể chỉ output 1 spike "i" rồi 8 frame pad → đếm liên tiếp ra
     * 20ms (hoàn toàn sai).
     *
     * ## Cách ước lượng đúng (emission-to-next-emission)
     *
     * Coi blank giữa 2 phoneme là phần kéo dài của phoneme trước (vì đó
     * chính là acoustic energy còn vang). Mỗi phoneme span:
     *
     *   start = frame nó spike
     *   end   = frame phoneme KẾ TIẾP spike (hoặc end of audio nếu cuối)
     *
     * Cách này không hoàn hảo (vẫn ôm cả silence cuối nếu là phoneme cuối),
     * nhưng cho **relative duration** đáng tin — đủ để phân biệt long/short
     * vowel trong cùng câu (so ratio).
     *
     * ## Trade-off
     *
     * Cách chính xác hơn là Viterbi force-alignment với CTC posterior, hoặc
     * "merge blanks evenly". Nhưng phức tạp hơn, và với mục tiêu chấm điểm
     * tương đối thì cách này đủ.
     *
     * @param frameMs ms/frame (wav2vec2 base: 320/16000 = 20 ms). Truyền 0
     *                nếu chỉ cần phoneme list (start/end sẽ = 0).
     */
    fun greedyDecodeWithTiming(
        logits: FloatArray,
        vocabSize: Int,
        vocab: Wav2Vec2Vocab,
        frameMs: Float,
    ): List<PhonemeFrame> {
        val timeSteps = logits.size / vocabSize
        if (timeSteps == 0) return emptyList()

        // Pass 1: argmax mỗi frame
        val argmax = IntArray(timeSteps)
        for (t in 0 until timeSteps) {
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
            argmax[t] = maxId
        }

        // Pass 2: CTC collapse — lấy emissions (token, startFrame) cho các
        // token thật (non-special), bỏ blank/special và bỏ duplicate liên tiếp.
        data class Emission(val tokenId: Int, val startFrame: Int)
        val emissions = ArrayList<Emission>(timeSteps / 4)
        var prevId = -1
        for (t in 0 until timeSteps) {
            val id = argmax[t]
            if (id != prevId && !vocab.isSpecial(id)) {
                emissions += Emission(id, t)
            }
            prevId = id
        }
        if (emissions.isEmpty()) return emptyList()

        // Pass 3: tính endFrame
        //
        // - phoneme không phải cuối: end = start của phoneme kế tiếp.
        // - phoneme cuối: KHÔNG dùng `timeSteps` (sẽ ôm trailing silence
        //   mà VAD để lại sau khi user dứt câu — thường vài trăm ms đến cả
        //   giây). Thay vào đó scan tới khi gặp "silence run" (≥ SILENCE_RUN_FRAMES
        //   blank liên tiếp) → cắt ngay đầu silence run đó. Nếu không tìm
        //   thấy, cap ở MAX_LAST_PHONEME_FRAMES.
        val frames = ArrayList<PhonemeFrame>(emissions.size)
        for (i in emissions.indices) {
            val em = emissions[i]
            val candidateEnd = if (i + 1 < emissions.size) {
                emissions[i + 1].startFrame
            } else {
                detectLastPhonemeEnd(argmax, em.startFrame, timeSteps) { id -> vocab.isSpecial(id) }
            }
            vocab.decode(em.tokenId)?.let { tok ->
                frames += PhonemeFrame(
                    phoneme = tok,
                    startMs = (em.startFrame * frameMs).toInt(),
                    endMs = (candidateEnd * frameMs).toInt(),
                )
            }
        }
        return frames
    }

    /** Pad run ≥ 10 frame (200ms) sau emission cuối → coi là người dùng dứt câu. */
    private const val SILENCE_RUN_FRAMES = 10

    /** Trần cứng cho phoneme cuối (250ms) khi không phát hiện silence run rõ. */
    private const val MAX_LAST_PHONEME_FRAMES = 13

    /**
     * Ước lượng end của phoneme cuối — tránh ôm trailing silence của VAD.
     *
     * Quét từ [startFrame] về phía trước:
     *   - Đếm pad/special liên tiếp.
     *   - Khi đạt [SILENCE_RUN_FRAMES] → end = vị trí bắt đầu của silence run.
     *   - Nếu hết audio mà không đạt → cap tại `startFrame + MAX_LAST_PHONEME_FRAMES`.
     */
    private fun detectLastPhonemeEnd(
        argmax: IntArray,
        startFrame: Int,
        timeSteps: Int,
        isSpecial: (Int) -> Boolean,
    ): Int {
        val hardCap = (startFrame + MAX_LAST_PHONEME_FRAMES).coerceAtMost(timeSteps)
        var padRun = 0
        var padRunStart = startFrame
        for (t in startFrame until timeSteps) {
            if (isSpecial(argmax[t])) {
                if (padRun == 0) padRunStart = t
                padRun++
                if (padRun >= SILENCE_RUN_FRAMES) {
                    return padRunStart.coerceAtMost(hardCap)
                }
            } else {
                padRun = 0
            }
        }
        return hardCap
    }
}
