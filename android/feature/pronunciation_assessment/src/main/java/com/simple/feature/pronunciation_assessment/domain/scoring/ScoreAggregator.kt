package com.simple.feature.pronunciation_assessment.domain.scoring

import com.simple.feature.pronunciation_assessment.domain.repositories.PhonemeDictionary
import com.simple.phonetics.entities.ErrorType
import com.simple.phonetics.entities.PhonemeScore
import com.simple.phonetics.entities.PronunciationError
import com.simple.phonetics.entities.SentenceScore
import com.simple.phonetics.entities.WordScore

/**
 * Tổng hợp [PhonemeScore] → [WordScore] → [SentenceScore].
 */
object ScoreAggregator {

    /**
     * Tổng hợp PhonemeScore → WordScore.
     * Bỏ qua INSERTION khi tính trung bình (người dùng thêm âm thừa không
     * penalize nặng bằng thiếu âm).
     */
    fun aggregateWord(word: String, phonemeScores: List<PhonemeScore>): WordScore {
        val scorable = phonemeScores.filter { it.errorType != ErrorType.INSERTION }
        val avg = if (scorable.isEmpty()) 0
        else scorable.sumOf { it.score } / scorable.size
        return WordScore(word, phonemeScores, avg)
    }

    /**
     * Tổng hợp WordScore → SentenceScore.
     *
     * Partial mode:
     *   - accuracyScore     = avg điểm chỉ phần đã đọc (bỏ phần chưa đọc)
     *   - completenessScore = coveredPhonemes / totalRef (thông tin, không penalize)
     *   - finalScore        = accuracyScore - fluencyPenalty
     *
     * Full mode:
     *   - finalScore        = accuracyScore × completeness - fluencyPenalty
     */
    fun aggregateSentence(
        referenceText: String,
        wordScores: List<WordScore>,
        totalReferencePhonemes: Int,
        coveredPhonemes: Int,
        fluencyPenalty: Int = 0,
        isPartial: Boolean = false,
    ): SentenceScore {
        val allPhonemeScores = wordScores.flatMap { it.phonemeScores }
            .filter { it.errorType != ErrorType.INSERTION }

        val accuracyScore = if (allPhonemeScores.isEmpty()) 0
        else allPhonemeScores.sumOf { it.score } / allPhonemeScores.size

        val completenessScore = if (totalReferencePhonemes == 0) 100
        else (coveredPhonemes * 100) / totalReferencePhonemes

        val rawFinal = if (isPartial) {
            accuracyScore - fluencyPenalty
        } else {
            (accuracyScore * completenessScore / 100) - fluencyPenalty
        }
        val finalScore = rawFinal.coerceIn(0, 100)

        val errors = wordScores.flatMap { ws ->
            ws.phonemeScores
                .filter { it.errorType != ErrorType.CORRECT && it.errorType != ErrorType.INSERTION }
                .map { ph ->
                    PronunciationError(
                        phoneme = ph.expected,
                        errorType = ph.errorType,
                        substitutedWith = if (ph.errorType == ErrorType.SUBSTITUTION) ph.actual else null,
                        wordContext = ws.word,
                    )
                }
        }

        return SentenceScore(
            referenceText = referenceText,
            wordScores = wordScores,
            accuracyScore = accuracyScore,
            completenessScore = completenessScore,
            fluencyPenalty = fluencyPenalty,
            finalScore = finalScore,
            errors = errors,
            isPartial = isPartial,
        )
    }
}

/**
 * Gán [PhonemeScore.grapheme] cho từng phoneme score dựa trên [PhonemeDictionary].
 *
 * Ví dụ với "knife" → chunks [("kn","n"), ("i","aɪ"), ("fe","f")]:
 *   phoneme index 0 ("n")  → grapheme "kn"
 *   phoneme index 1 ("aɪ") → grapheme "i"
 *   phoneme index 2 ("f")  → grapheme "fe"
 *
 * Nếu từ không có trong dict → tất cả phoneme của từ đó nhận grapheme = word.
 */
internal fun resolveGraphemes(
    word: String,
    slice: List<PhonemeScore>,
    dict: PhonemeDictionary,
): List<PhonemeScore> {
    if (slice.isEmpty()) return slice
    val chunks = dict[word]
        ?: return slice.map { it.copy(grapheme = word) }

    val graphemePerPhoneme = buildList {
        for (chunk in chunks) {
            val atomicCount = PhonemeTokenizer.tokenize(chunk.phoneme).size
            repeat(atomicCount) { add(chunk.grapheme) }
        }
    }

    return slice.mapIndexed { idx, ps ->
        ps.copy(grapheme = graphemePerPhoneme.getOrNull(idx))
    }
}
