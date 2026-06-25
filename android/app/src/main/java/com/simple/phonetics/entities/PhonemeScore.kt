package com.simple.phonetics.entities
// ─────────────────────────────────────────────
// Data classes
// ─────────────────────────────────────────────

/** Kết quả điểm của một phoneme */
data class PhonemeScore(
    val expected: String = "",        // Phoneme chuẩn,  ví dụ "/æ/"
    val actual: String? = "",         // Phoneme người dùng phát ra (null = thiếu)
    val score: Int = 0,              // 0–100

    val errorType: ErrorType = ErrorType.SUBSTITUTION,

    /**
     * Cụm ký tự (grapheme) tương ứng từ PhonemeDict, ví dụ "th", "ea", "kn".
     * null nếu không có PhonemeDict hoặc từ không có trong dictionary.
     */
    val grapheme: String? = null
)

enum class ErrorType { CORRECT, SUBSTITUTION, DELETION, INSERTION }

/** Kết quả điểm của một từ */
data class WordScore(
    val word: String = "",
    val phonemeScores: List<PhonemeScore> = emptyList(),
    val score: Int = 0             // avg của phoneme scores
)

/** Kết quả điểm toàn câu */
data class SentenceScore(
    val referenceText: String = "",
    val wordScores: List<WordScore> = emptyList(),
    val accuracyScore: Int = 0,      // avg(phoneme scores)
    val completenessScore: Int = 0,  // % âm đã đọc / tổng âm
    val fluencyPenalty: Int = 0,     // trừ điểm nếu dừng nhiều
    val finalScore: Int = 0,         // điểm cuối
    val errors: List<PronunciationError> = emptyList(),
    val isPartial: Boolean = true,   // true nếu người dùng chưa đọc hết câu
    val audioFilePath: String? = null // đường dẫn file WAV người dùng vừa đọc (chỉ có ở kết quả final)
)

data class PronunciationError(
    val phoneme: String = "",
    val errorType: ErrorType = ErrorType.SUBSTITUTION,
    val substitutedWith: String? = null,
    val wordContext: String = ""
)