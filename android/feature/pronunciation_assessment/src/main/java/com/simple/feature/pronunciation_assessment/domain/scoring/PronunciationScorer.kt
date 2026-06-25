package com.simple.feature.pronunciation_assessment.domain.scoring

import com.simple.feature.pronunciation_assessment.domain.repositories.PhonemeDictionary
import com.simple.phonetics.entities.ErrorType
import com.simple.phonetics.entities.PhonemeScore
import com.simple.phonetics.entities.SentenceScore
import com.simple.phonetics.entities.WordScore

/**
 * PronunciationScorer — orchestrate scoring pipeline:
 *   1. Align hypothesis vs reference (partial-friendly)
 *   2. GOP cho từng phoneme
 *   3. Map về từng từ + điền grapheme nếu có dict
 *   4. Aggregate thành [SentenceScore]
 *
 * Class này pure logic — không đụng Android/IO.
 */
class PronunciationScorer {

    /** Chấm điểm full — người dùng đã đọc xong câu. */
    fun score(
        wordPhonemes: List<Pair<String, List<String>>>,
        spokenPhonemes: List<String>,
        fluencyPenalty: Int = 0,
        noiseLevel: Int = 0,
        phonemeDict: PhonemeDictionary? = null,
    ): SentenceScore = scoreInternal(
        wordPhonemes = wordPhonemes,
        spokenPhonemes = spokenPhonemes,
        fluencyPenalty = fluencyPenalty,
        noiseLevel = noiseLevel,
        isPartial = false,
        phonemeDict = phonemeDict,
    )

    /** Chấm điểm partial — người dùng chưa đọc hết câu. */
    fun scorePartial(
        wordPhonemes: List<Pair<String, List<String>>>,
        spokenPhonemes: List<String>,
        fluencyPenalty: Int = 0,
        noiseLevel: Int = 0,
        phonemeDict: PhonemeDictionary? = null,
    ): SentenceScore = scoreInternal(
        wordPhonemes = wordPhonemes,
        spokenPhonemes = spokenPhonemes,
        fluencyPenalty = fluencyPenalty,
        noiseLevel = noiseLevel,
        isPartial = true,
        phonemeDict = phonemeDict,
    )

    // ── private core ──────────────────────────

    private fun scoreInternal(
        wordPhonemes: List<Pair<String, List<String>>>,
        spokenPhonemes: List<String>,
        fluencyPenalty: Int,
        noiseLevel: Int,
        isPartial: Boolean,
        phonemeDict: PhonemeDictionary?,
    ): SentenceScore {
        val referenceText = wordPhonemes.joinToString(" ") { it.first }
        val referencePhonemes = wordPhonemes.flatMap { it.second }
        val totalRef = referencePhonemes.size

        // 1. Alignment — alignPartial dùng cho cả full (iBest = m khi đọc đủ)
        val alignResult = PhonemeAligner.alignPartial(referencePhonemes, spokenPhonemes)
        val coveredUpto = alignResult.coveredUpto

        // 2. GOP cho phần đã align
        val allPhonemeScores = alignResult.pairs
            .filter { it.reference != null } // bỏ pure insertion
            .map { pair ->
                val expected = pair.reference!!
                val actual = pair.hypothesis
                PhonemeScore(
                    expected = expected,
                    actual = actual,
                    score = GOPScorer.score(expected, actual, noiseLevel),
                    errorType = GOPScorer.errorType(expected, actual),
                )
            }

        // 3. Map phoneme scores về từng từ
        val wordScores = buildWordScores(wordPhonemes, allPhonemeScores, coveredUpto, phonemeDict)

        // 4. Aggregate
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
     * Phân bổ phonemeScores về đúng WordScore.
     *
     * Từ nào bắt đầu sau coveredUpto → chưa đọc → không đưa vào kết quả.
     * Từ nào bị cắt ngang → đưa vào với phần đã có.
     */
    private fun buildWordScores(
        wordPhonemes: List<Pair<String, List<String>>>,
        allPhonemeScores: List<PhonemeScore>,
        coveredUpto: Int,
        phonemeDict: PhonemeDictionary?,
    ): List<WordScore> {
        var cursor = 0
        val result = mutableListOf<WordScore>()

        for ((word, phonemes) in wordPhonemes) {
            val wordStart = cursor
            val wordEnd = cursor + phonemes.size
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
