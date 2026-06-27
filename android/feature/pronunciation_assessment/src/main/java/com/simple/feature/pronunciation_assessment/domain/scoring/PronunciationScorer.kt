package com.simple.feature.pronunciation_assessment.domain.scoring

import com.simple.feature.pronunciation_assessment.domain.entities.PhonemeFrame
import com.simple.feature.pronunciation_assessment.domain.repositories.PhonemeDictionary
import com.simple.phonetics.entities.ErrorType
import com.simple.phonetics.entities.PhonemeScore
import com.simple.phonetics.entities.SentenceScore
import com.simple.phonetics.entities.WordScore

/**
 * PronunciationScorer — orchestrate scoring pipeline:
 *   1. Normalize ː khỏi cả reference & hypothesis → bases + isLong metadata
 *   2. Align bases (partial-friendly)
 *   3. GOP cho từng phoneme
 *   4. Áp length penalty cho vowel có duration không khớp expected
 *   5. Map về từng từ + điền grapheme nếu có dict
 *   6. Aggregate thành [SentenceScore]
 *
 * Class này pure logic — không đụng Android/IO.
 */
class PronunciationScorer {

    /** Chấm điểm full — người dùng đã đọc xong câu. */
    fun score(
        wordPhonemes: List<Pair<String, List<String>>>,
        spokenPhonemes: List<String>,
        spokenFrames: List<PhonemeFrame> = emptyList(),
        fluencyPenalty: Int = 0,
        noiseLevel: Int = 0,
        phonemeDict: PhonemeDictionary? = null,
    ): SentenceScore = scoreInternal(
        wordPhonemes = wordPhonemes,
        spokenPhonemes = spokenPhonemes,
        spokenFrames = spokenFrames,
        fluencyPenalty = fluencyPenalty,
        noiseLevel = noiseLevel,
        isPartial = false,
        phonemeDict = phonemeDict,
    )

    /** Chấm điểm partial — người dùng chưa đọc hết câu. */
    fun scorePartial(
        wordPhonemes: List<Pair<String, List<String>>>,
        spokenPhonemes: List<String>,
        spokenFrames: List<PhonemeFrame> = emptyList(),
        fluencyPenalty: Int = 0,
        noiseLevel: Int = 0,
        phonemeDict: PhonemeDictionary? = null,
    ): SentenceScore = scoreInternal(
        wordPhonemes = wordPhonemes,
        spokenPhonemes = spokenPhonemes,
        spokenFrames = spokenFrames,
        fluencyPenalty = fluencyPenalty,
        noiseLevel = noiseLevel,
        isPartial = true,
        phonemeDict = phonemeDict,
    )

    // ── private core ──────────────────────────

    private fun scoreInternal(
        wordPhonemes: List<Pair<String, List<String>>>,
        spokenPhonemes: List<String>,
        spokenFrames: List<PhonemeFrame>,
        fluencyPenalty: Int,
        noiseLevel: Int,
        isPartial: Boolean,
        phonemeDict: PhonemeDictionary?,
    ): SentenceScore {
        val referenceText = wordPhonemes.joinToString(" ") { it.first }

        // 1. Normalize reference: ː → metadata isLong của vowel trước
        // Áp dụng từng-từ để giữ wordBoundaries chính xác sau khi normalize.
        val normalizedWordRefs: List<Pair<String, List<LengthNormalizer.Norm>>> =
            wordPhonemes.map { (w, p) -> w to LengthNormalizer.normalize(p) }
        val referenceNorms = normalizedWordRefs.flatMap { it.second }
        val referenceBases = referenceNorms.map { it.base }
        val totalRef = referenceBases.size

        // 2. Normalize hypothesis (tách ː khỏi token nếu có)
        val hypothesisNorms = LengthNormalizer.normalize(spokenPhonemes)
        val hypothesisBases = hypothesisNorms.map { it.base }

        // 3. Map base index → frame thực tế (nếu recognizer có cung cấp).
        //    Frame list trước normalize có thể có số phần tử khác hypothesisBases
        //    (do "ː" riêng bị skip / "iː" được split). Match theo thứ tự — bỏ frame
        //    nào phoneme là pure "ː", các frame còn lại ánh xạ 1-1 với base.
        val hypothesisFrames: List<PhonemeFrame?> = mapFramesToBases(spokenFrames, hypothesisBases.size)

        val avgVowelMs = VowelLengthScorer.averageVowelDurationMs(spokenFrames)

        // 4. Alignment dựa trên bases
        val alignResult = PhonemeAligner.alignPartial(referenceBases, hypothesisBases)
        val coveredUpto = alignResult.coveredUpto

        // 5. Build phoneme scores — kèm length penalty cho vowel.
        //    Cần biết vị trí ref/hyp gốc của từng pair để truy isLong + duration.
        //    Backtrack lại pairs với index tracking.
        val allPhonemeScores = buildPhonemeScores(
            pairs = alignResult.pairs,
            referenceNorms = referenceNorms,
            hypothesisNorms = hypothesisNorms,
            hypothesisFrames = hypothesisFrames,
            avgVowelMs = avgVowelMs,
            noiseLevel = noiseLevel,
        )

        // 6. Map phoneme scores về từng từ
        val wordScores = buildWordScores(normalizedWordRefs, allPhonemeScores, coveredUpto, phonemeDict)

        // 7. Aggregate
        return ScoreAggregator.aggregateSentence(
            referenceText = referenceText,
            wordScores = wordScores,
            totalReferencePhonemes = totalRef,
            coveredPhonemes = coveredUpto,
            fluencyPenalty = fluencyPenalty,
            isPartial = isPartial,
        )
    }

    /**
     * Walk qua aligned pairs theo thứ tự, dùng 2 con trỏ ref/hyp để biết
     * phần tử nào trong [referenceNorms] / [hypothesisNorms] tương ứng.
     * Tại mỗi cặp:
     *   - GOP score trên `base`
     *   - Nếu cặp là vowel và base match → áp length penalty nếu isLong khác nhau
     *     dựa trên duration thực tế từ [hypothesisFrames] (so với avgVowelMs).
     */
    private fun buildPhonemeScores(
        pairs: List<PhonemeAligner.AlignedPair>,
        referenceNorms: List<LengthNormalizer.Norm>,
        hypothesisNorms: List<LengthNormalizer.Norm>,
        hypothesisFrames: List<PhonemeFrame?>,
        avgVowelMs: Float,
        noiseLevel: Int,
    ): List<PhonemeScore> {
        val out = ArrayList<PhonemeScore>(pairs.size)
        var refIdx = 0
        var hypIdx = 0

        for (pair in pairs) {
            when {
                pair.reference != null && pair.hypothesis != null -> {
                    val refNorm = referenceNorms.getOrNull(refIdx)
                    val hypNorm = hypothesisNorms.getOrNull(hypIdx)
                    val frame = hypothesisFrames.getOrNull(hypIdx)

                    val expectedDisplay = displayForm(refNorm)
                    val actualDisplay = displayForm(hypNorm)

                    val baseScore = GOPScorer.score(pair.reference, pair.hypothesis, noiseLevel)
                    val baseError = GOPScorer.errorType(pair.reference, pair.hypothesis)

                    // Length scoring — chỉ áp dụng cho VOWEL khi base match (CORRECT).
                    //   - Sai base → đã có penalty SUBSTITUTION rồi.
                    //   - Consonant không có khái niệm long/short trong tiếng Anh
                    //     (không có /tː/ /mː/ ...) → bỏ qua hoàn toàn.
                    val lengthRes = if (baseError == ErrorType.CORRECT
                        && refNorm != null
                        && VowelLengthScorer.isVowel(refNorm.base)
                    ) {
                        VowelLengthScorer.evaluate(
                            expectedLong = refNorm.isLong,
                            actualMs = frame?.durationMs ?: 0,
                            avgVowelMs = avgVowelMs,
                        )
                    } else null

                    val hasLengthIssue = lengthRes != null &&
                        lengthRes.verdict != VowelLengthScorer.LengthVerdict.OK &&
                        lengthRes.verdict != VowelLengthScorer.LengthVerdict.UNKNOWN

                    val finalScore = (baseScore - (lengthRes?.penalty ?: 0)).coerceIn(0, 100)

                    // Khi sai length, đổi errorType → SUBSTITUTION để UI flag được.
                    // actualDisplay reflect đúng đặc tính long/short người dùng nói.
                    val effectiveError = if (hasLengthIssue) ErrorType.SUBSTITUTION else baseError
                    val effectiveActual = if (hasLengthIssue) {
                        // Show actual với length detected: long nếu họ nói long, vice versa
                        val actualLong = !refNorm!!.isLong  // length sai = ngược với expected
                        if (actualLong) hypNorm!!.base + "ː" else hypNorm!!.base
                    } else actualDisplay

                    out += PhonemeScore(
                        expected = expectedDisplay,
                        actual = effectiveActual,
                        score = finalScore,
                        errorType = effectiveError,
                    )

                    refIdx++; hypIdx++
                }
                pair.reference != null && pair.hypothesis == null -> {
                    // deletion
                    val refNorm = referenceNorms.getOrNull(refIdx)
                    out += PhonemeScore(
                        expected = displayForm(refNorm),
                        actual = null,
                        score = 0,
                        errorType = ErrorType.DELETION,
                    )
                    refIdx++
                }
                pair.reference == null && pair.hypothesis != null -> {
                    // insertion — bỏ qua như cũ (pure insertion không add vào)
                    hypIdx++
                }
            }
        }
        return out
    }

    /** Khôi phục ː cho display nếu phoneme đó là long. */
    private fun displayForm(norm: LengthNormalizer.Norm?): String {
        if (norm == null) return ""
        return if (norm.isLong) norm.base + "ː" else norm.base
    }

    /**
     * Ánh xạ frames (từ CTC) → list cùng kích thước với hypothesisBases.
     *
     * - Frames có phoneme == "ː" đơn lẻ → bỏ (do normalize cũng strip).
     * - Frames có hậu tố "ː" (vd "iː") → giữ, ánh xạ 1-1 với base tương ứng.
     * - Frames khác → giữ 1-1.
     * Nếu length không khớp (rare) → fallback null cho phần thiếu.
     */
    private fun mapFramesToBases(
        frames: List<PhonemeFrame>,
        baseCount: Int,
    ): List<PhonemeFrame?> {
        if (frames.isEmpty()) return List(baseCount) { null }
        val mapped = ArrayList<PhonemeFrame>(baseCount)
        for (f in frames) {
            if (f.phoneme == "ː") continue
            mapped += f
        }
        return List(baseCount) { mapped.getOrNull(it) }
    }

    /**
     * Phân bổ phonemeScores về đúng WordScore.
     *
     * Từ nào bắt đầu sau coveredUpto → chưa đọc → không đưa vào kết quả.
     * Từ nào bị cắt ngang → đưa vào với phần đã có.
     */
    private fun buildWordScores(
        normalizedWordRefs: List<Pair<String, List<LengthNormalizer.Norm>>>,
        allPhonemeScores: List<PhonemeScore>,
        coveredUpto: Int,
        phonemeDict: PhonemeDictionary?,
    ): List<WordScore> {
        var cursor = 0
        val result = mutableListOf<WordScore>()

        for ((word, norms) in normalizedWordRefs) {
            val wordStart = cursor
            val wordEnd = cursor + norms.size
            cursor = wordEnd

            if (wordStart >= coveredUpto) break

            val slice = allPhonemeScores.subList(
                wordStart.coerceAtMost(allPhonemeScores.size),
                wordEnd.coerceAtMost(allPhonemeScores.size),
            )

            val sliceWithGrapheme = if (phonemeDict != null) {
                resolveGraphemes(word, slice, phonemeDict)
            } else {
                slice
            }

            result += ScoreAggregator.aggregateWord(word, sliceWithGrapheme)
        }
        return result
    }
}

/** Cast helper — dùng cho code cần đếm phoneme thực tế người dùng phát ra. */
internal fun List<PhonemeScore>.spokenCount(): Int =
    count { it.errorType != ErrorType.DELETION }
