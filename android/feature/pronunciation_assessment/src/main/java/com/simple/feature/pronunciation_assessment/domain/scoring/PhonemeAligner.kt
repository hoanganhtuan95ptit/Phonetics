package com.simple.feature.pronunciation_assessment.domain.scoring

import com.simple.feature.pronunciation_assessment.domain.scoring.PhonemeAligner.alignPartial


/**
 * Align hai chuỗi phoneme (reference vs hypothesis) bằng Levenshtein DP.
 *
 * Cost: substitution = 2, deletion = 3, insertion = 3.
 * Deletion nặng hơn substitution vì nuốt âm tệ hơn phát sai.
 */
object PhonemeAligner {

    /** Một cặp (ref, hyp) đã align. null = insertion/deletion. */
    data class AlignedPair(
        val reference: String?,  // null = insertion từ người dùng
        val hypothesis: String?, // null = deletion (âm bị thiếu)
    )

    /**
     * Kết quả [alignPartial].
     *
     * @param pairs        danh sách (ref, hyp) của phần đã được phủ
     * @param coveredUpto  index trong reference — phần [0, coveredUpto) đã
     *                     được hypothesis cover. Phần còn lại bỏ qua.
     */
    data class PartialAlignResult(
        val pairs: List<AlignedPair>,
        val coveredUpto: Int,
    )

    /** Full alignment — người dùng đã đọc xong câu. */
    fun align(
        reference: List<String>,
        hypothesis: List<String>,
    ): List<AlignedPair> = alignPartial(reference, hypothesis).pairs

    /**
     * Partial alignment — chỉ chấm những gì người dùng đã đọc.
     *
     * Thuật toán "free deletion ở cuối ref":
     *   1. Chạy DP bình thường trên toàn bộ ref × hyp.
     *   2. Khi hypothesis hết (j = n), không bắt buộc phải tiêu thụ hết
     *      reference — tìm i* mà dp[i*][n] nhỏ nhất.
     *   3. Backtrack từ (i*, n) — phần ref[i*..end] = chưa đọc → bỏ qua.
     */
    fun alignPartial(
        reference: List<String>,
        hypothesis: List<String>,
    ): PartialAlignResult {
        val m = reference.size
        val n = hypothesis.size

        if (n == 0) return PartialAlignResult(emptyList(), 0)

        // ── Build DP table ──────────────────────────────
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i * 3
        for (j in 0..n) dp[0][j] = j * 3

        for (i in 1..m) {
            for (j in 1..n) {
                val match = if (reference[i - 1] == hypothesis[j - 1]) 0 else 2
                dp[i][j] = minOf(
                    dp[i - 1][j - 1] + match, // match / substitution
                    dp[i - 1][j] + 3,         // deletion
                    dp[i][j - 1] + 3,         // insertion
                )
            }
        }

        // ── Tìm i* tốt nhất ở cột j = n ──────────────────
        var iBest = 0
        var bestCost = dp[0][n]
        for (i in 1..m) {
            if (dp[i][n] < bestCost) {
                bestCost = dp[i][n]
                iBest = i
            }
        }

        // ── Backtrack từ (iBest, n) ─────────────────────
        val pairs = mutableListOf<AlignedPair>()
        var i = iBest
        var j = n
        while (i > 0 || j > 0) {
            val matchCost = if (i > 0 && j > 0) {
                if (reference[i - 1] == hypothesis[j - 1]) 0 else 2
            } else Int.MAX_VALUE

            when {
                i > 0 && j > 0 && dp[i][j] == dp[i - 1][j - 1] + matchCost -> {
                    pairs += AlignedPair(reference[i - 1], hypothesis[j - 1])
                    i--; j--
                }
                i > 0 && dp[i][j] == dp[i - 1][j] + 3 -> {
                    pairs += AlignedPair(reference[i - 1], null) // deletion
                    i--
                }
                else -> {
                    pairs += AlignedPair(null, hypothesis[j - 1]) // insertion
                    j--
                }
            }
        }

        return PartialAlignResult(
            pairs = pairs.reversed(),
            coveredUpto = iBest,
        )
    }
}
