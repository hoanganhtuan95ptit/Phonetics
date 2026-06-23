package com.simple.phonetics.ui.speak.services.pronunciation_assessment.data.use_case

/**
 * PronunciationScorer.kt
 *
 * Pipeline chấm điểm phát âm tiếng Anh theo thuật toán GOP
 * (Goodness of Pronunciation), hỗ trợ partial sentence.
 *
 * Các bước:
 *   1. G2P  — chuyển text tham chiếu → chuỗi phoneme chuẩn
 *   2. Align — căn chỉnh phoneme người dùng vs chuẩn (edit distance)
 *   3. GOP  — tính điểm từng phoneme (0–100)
 *   4. Aggregate — tổng hợp điểm từng từ, câu, completeness
 *
 * Trong thực tế:
 *   - G2P     → thay bằng gọi epitran/g2p-en qua server hoặc CMU Dict lookup
 *   - Phoneme → thay bằng output của wav2vec2 (OnnxRuntime trên Android)
 *   - GOP log prob → thay bằng softmax scores từ acoustic model
 */

// ─────────────────────────────────────────────
// Data classes
// ─────────────────────────────────────────────

/** Kết quả điểm của một phoneme */
data class PhonemeScore(
    val expected: String,        // Phoneme chuẩn,  ví dụ "/æ/"
    val actual: String?,         // Phoneme người dùng phát ra (null = thiếu)
    val score: Int,              // 0–100
    val errorType: ErrorType
)

enum class ErrorType { CORRECT, SUBSTITUTION, DELETION, INSERTION }

/** Kết quả điểm của một từ */
data class WordScore(
    val word: String,
    val phonemeScores: List<PhonemeScore>,
    val score: Int               // avg của phoneme scores
)

/** Kết quả điểm toàn câu */
data class SentenceScore(
    val referenceText: String,
    val wordScores: List<WordScore>,
    val accuracyScore: Int,      // avg(phoneme scores)
    val completenessScore: Int,  // % âm đã đọc / tổng âm
    val fluencyPenalty: Int,     // trừ điểm nếu dừng nhiều
    val finalScore: Int,         // điểm cuối
    val errors: List<PronunciationError>,
    val isPartial: Boolean       // true nếu người dùng chưa đọc hết câu
)

data class PronunciationError(
    val phoneme: String,
    val errorType: ErrorType,
    val substitutedWith: String? = null,
    val wordContext: String = ""
)

// ─────────────────────────────────────────────
// Bước 1 — G2P: text → phoneme chuẩn
// ─────────────────────────────────────────────

/**
 * G2PConverter: Grapheme-to-Phoneme
 *
 * Trong production: thay bằng gọi server Python chạy epitran hoặc
 * tra CMU Pronouncing Dictionary được bundle vào app (~3MB).
 * Ở đây dùng bảng tra cứu nhỏ để demo pipeline.
 */
object G2PConverter {

    // Bảng tra cứu CMU-style IPA rút gọn — thêm từ tuỳ nhu cầu
    private val dict: Map<String, List<String>> = mapOf(
        "the"     to listOf("ð", "ə"),
        "a"       to listOf("ə"),
        "cat"     to listOf("k", "æ", "t"),
        "sat"     to listOf("s", "æ", "t"),
        "bat"     to listOf("b", "æ", "t"),
        "hat"     to listOf("h", "æ", "t"),
        "mat"     to listOf("m", "æ", "t"),
        "hello"   to listOf("h", "ɛ", "l", "oʊ"),
        "world"   to listOf("w", "ɜː", "l", "d"),
        "three"   to listOf("θ", "r", "iː"),
        "this"    to listOf("ð", "ɪ", "s"),
        "that"    to listOf("ð", "æ", "t"),
        "think"   to listOf("θ", "ɪ", "ŋ", "k"),
        "is"      to listOf("ɪ", "z"),
        "good"    to listOf("ɡ", "ʊ", "d"),
        "morning" to listOf("m", "ɔː", "r", "n", "ɪ", "ŋ"),
        "how"     to listOf("h", "aʊ"),
        "are"     to listOf("ɑː", "r"),
        "you"     to listOf("j", "uː"),
        "nice"    to listOf("n", "aɪ", "s"),
        "to"      to listOf("t", "uː"),
        "meet"    to listOf("m", "iː", "t"),
        "thank"   to listOf("θ", "æ", "ŋ", "k"),
        "very"    to listOf("v", "ɛ", "r", "i"),
        "much"    to listOf("m", "ʌ", "tʃ"),
    )

    /**
     * Chuyển câu → danh sách (word, phonemes).
     * Trả về null phonemes nếu từ không có trong dict.
     */
    fun convert(sentence: String): List<Pair<String, List<String>>> {
        return sentence
            .lowercase()
            .replace(Regex("[^a-z\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .map { word -> word to (dict[word] ?: listOf("?")) }
    }

    fun isKnown(word: String): Boolean = dict.containsKey(word.lowercase())
}

// ─────────────────────────────────────────────
// Bước 2 — Alignment: edit distance + backtrack
// ─────────────────────────────────────────────

data class AlignedPair(
    val reference: String?,  // null = insertion từ người dùng
    val hypothesis: String?  // null = deletion (âm bị thiếu)
)

/**
 * Kết quả partial alignment.
 *
 * @param pairs        Danh sách (ref, hyp) của phần đã được phủ
 * @param coveredUpto  Index trong reference — phần [0, coveredUpto)
 *                     đã được hypothesis cover. Phần còn lại bỏ qua.
 */
data class PartialAlignResult(
    val pairs: List<AlignedPair>,
    val coveredUpto: Int
)

object PhonemeAligner {

    /**
     * Full alignment (người dùng đã đọc xong câu).
     *
     * Levenshtein DP, cost: substitution=2, deletion=3, insertion=3.
     * Deletion nặng hơn substitution vì nuốt âm tệ hơn phát sai.
     */
    fun align(
        reference: List<String>,
        hypothesis: List<String>
    ): List<AlignedPair> {
        return alignPartial(reference, hypothesis).pairs
    }

    /**
     * Partial alignment — chỉ chấm những gì người dùng đã đọc.
     *
     * Vấn đề với align() thông thường khi dùng cho partial:
     *   ref  = [ð, ə, k, æ, t, s, æ, t]   ("the cat sat")
     *   hyp  = [d, ə, k, æ, t]             (người dùng mới đọc "the cat")
     *   → align() tính s, æ, t là DELETION dù chưa đến lượt đọc!
     *
     * Thuật toán đúng — "free deletion ở cuối ref":
     *   1. Chạy DP bình thường trên toàn bộ ref × hyp.
     *   2. Khi hypothesis hết (j = n), không bắt buộc phải tiêu thụ
     *      hết reference — tìm i* mà dp[i*][n] nhỏ nhất.
     *      i* là điểm trong reference mà hypothesis kết thúc tốt nhất.
     *   3. Backtrack từ (i*, n) — phần ref[i*..end] = chưa đọc → bỏ qua.
     *
     * Ví dụ:
     *   dp[5][5] = 2  (align "the cat" với [d,ə,k,æ,t] — chỉ 1 lỗi /ð/→/d/)
     *   dp[8][5] = 11 (buộc align cả "sat" → 3 deletion thêm)
     *   → chọn i*=5, backtrack từ đó, bỏ "sat"
     */
    fun alignPartial(
        reference: List<String>,
        hypothesis: List<String>
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
                val match = if (reference[i-1] == hypothesis[j-1]) 0 else 2
                dp[i][j] = minOf(
                    dp[i-1][j-1] + match,  // match / substitution
                    dp[i-1][j]   + 3,      // deletion
                    dp[i][j-1]   + 3       // insertion
                )
            }
        }

        // ── Tìm i* — điểm reference tốt nhất khi hypothesis hết ──
        // Scan cột j=n, tìm row i có dp[i][n] nhỏ nhất.
        // Nếu nhiều i có cùng cost, ưu tiên i lớn hơn (phủ nhiều hơn).
        var iBest = 0
        var bestCost = dp[0][n]
        for (i in 1..m) {
            if (dp[i][n] < bestCost) {
                bestCost = dp[i][n]
                iBest = i
            }
        }

        // ── Backtrack từ (iBest, n) ──────────────────────
        val pairs = mutableListOf<AlignedPair>()
        var i = iBest; var j = n
        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && dp[i][j] == dp[i-1][j-1] + (if (reference[i-1] == hypothesis[j-1]) 0 else 2) -> {
                    pairs.add(AlignedPair(reference[i-1], hypothesis[j-1]))
                    i--; j--
                }
                i > 0 && dp[i][j] == dp[i-1][j] + 3 -> {
                    pairs.add(AlignedPair(reference[i-1], null))  // deletion trong phần đã đọc
                    i--
                }
                else -> {
                    pairs.add(AlignedPair(null, hypothesis[j-1]))  // insertion
                    j--
                }
            }
        }

        return PartialAlignResult(
            pairs      = pairs.reversed(),
            coveredUpto = iBest   // ref[0..iBest) đã được phủ
        )
    }
}

// ─────────────────────────────────────────────
// Bước 3 — GOP: tính điểm từng phoneme
// ─────────────────────────────────────────────

/**
 * PhonemeFeatures — mô tả đặc trưng âm vị học của mỗi phoneme.
 * Dùng để tính confusion cost theo khoảng cách phonetic thực sự,
 * không phụ thuộc quốc tịch người dùng.
 */

// ── Consonant features ────────────────────────

enum class Manner {
    PLOSIVE, FRICATIVE, AFFRICATE, NASAL, APPROXIMANT, LATERAL, RHOTIC, GLIDE
}

enum class Place {
    BILABIAL, LABIODENTAL, DENTAL, ALVEOLAR, POSTALVEOLAR, PALATAL, VELAR, GLOTTAL
}

data class ConsonantFeatures(
    val manner: Manner,
    val place:  Place,
    val voiced: Boolean
)

// ── Vowel features ────────────────────────────

enum class VowelHeight(val level: Int) { HIGH(3), MID(2), LOW(1) }
enum class VowelBackness(val level: Int) { FRONT(1), CENTRAL(2), BACK(3) }

data class VowelFeatures(
    val height:    VowelHeight,
    val backness:  VowelBackness,
    val rounded:   Boolean,
    val tense:     Boolean,
    val diphthong: Boolean = false
)

// ── Distance tables ───────────────────────────

/**
 * Khoảng cách giữa 2 Manner (0 = giống, 4 = rất khác).
 * Symmetric — chỉ lưu (min, max) index.
 */
private val MANNER_DIST: Map<Pair<Int,Int>, Int> = buildMap {
    // Upper-triangle (i ≤ j) — stored once, looked up symmetrically
    //            PLOS FRIC AFFC NASL APRX LATL RHOT GLID
    val raw = listOf(
        Pair(0,0) to 0,  Pair(0,1) to 3,  Pair(0,2) to 2,  Pair(0,3) to 3,
        Pair(0,4) to 4,  Pair(0,5) to 4,  Pair(0,6) to 4,  Pair(0,7) to 4,
        Pair(1,1) to 0,  Pair(1,2) to 2,  Pair(1,3) to 4,  Pair(1,4) to 3,
        Pair(1,5) to 3,  Pair(1,6) to 3,  Pair(1,7) to 3,
        Pair(2,2) to 0,  Pair(2,3) to 4,  Pair(2,4) to 4,  Pair(2,5) to 4,
        Pair(2,6) to 4,  Pair(2,7) to 4,
        Pair(3,3) to 0,  Pair(3,4) to 2,  Pair(3,5) to 2,  Pair(3,6) to 3,  Pair(3,7) to 3,
        Pair(4,4) to 0,  Pair(4,5) to 1,  Pair(4,6) to 1,  Pair(4,7) to 1,
        Pair(5,5) to 0,  Pair(5,6) to 1,  Pair(5,7) to 2,
        Pair(6,6) to 0,  Pair(6,7) to 2,
        Pair(7,7) to 0,
    )
    raw.forEach { (k, v) ->
        put(k, v)
        put(Pair(k.second, k.first), v)  // symmetric
    }
}

private fun mannerDist(a: Manner, b: Manner): Int =
    MANNER_DIST[Pair(a.ordinal, b.ordinal)] ?: 5

private fun placeDist(a: Place, b: Place): Int =
    kotlin.math.abs(a.ordinal - b.ordinal)

/**
 * Tính confusion cost giữa 2 consonant dựa trên phonetic features.
 *
 * Weights (dựa trên linguistic và acoustic research):
 *   voicing     = 15   (voiced/unvoiced — dễ nhầm, ít ảnh hưởng nhất)
 *   manner_step = 10   (cách phát âm — quan trọng hơn)
 *   place_step  = 7    (vị trí phát âm — ảnh hưởng vừa)
 *
 * Ví dụ:
 *   /p/→/b/ = voicing only           = 15  → score 85
 *   /p/→/t/ = same manner, 3 place   = 21  → score 79
 *   /θ/→/t/ = differ manner+place    = 37  → score 63
 *   /l/→/r/ = lateral vs rhotic      = 10  → score 90
 */
private fun consonantCost(f1: ConsonantFeatures, f2: ConsonantFeatures): Int {
    val voicingCost = if (f1.voiced == f2.voiced) 0 else 15
    val mannerCost  = mannerDist(f1.manner, f2.manner) * 10
    val placeCost   = placeDist(f1.place,  f2.place)   * 7
    return (voicingCost + mannerCost + placeCost).coerceAtMost(95)
}

/**
 * Tính confusion cost giữa 2 vowel.
 *
 * Weights:
 *   height_step   = 18  (high/mid/low — nguyên âm quan trọng nhất)
 *   backness_step = 14  (front/central/back)
 *   rounded       = 10
 *   tense         = 8   (iː vs ɪ, uː vs ʊ)
 *   diphthong     = 14  (monophthong vs diphthong)
 *
 * Ví dụ:
 *   /iː/→/ɪ/  = tense only           = 8   → score 92
 *   /æ/→/ɛ/   = 1 height step        = 18  → score 82
 *   /iː/→/uː/ = 2 backness steps     = 28  → score 72
 *   /aɪ/→/aʊ/ = 2 backness diph      = 28  → score 72
 */
private fun vowelCost(v1: VowelFeatures, v2: VowelFeatures): Int {
    val heightCost   = kotlin.math.abs(v1.height.level   - v2.height.level)   * 18
    val backnessCost = kotlin.math.abs(v1.backness.level - v2.backness.level)  * 14
    val roundCost    = if (v1.rounded   == v2.rounded)   0 else 10
    val tenseCost    = if (v1.tense     == v2.tense)     0 else 8
    val diphCost     = if (v1.diphthong == v2.diphthong) 0 else 14
    return (heightCost + backnessCost + roundCost + tenseCost + diphCost).coerceAtMost(90)
}

// ── IPA phoneme table (English) ───────────────

private val CONSONANT_TABLE: Map<String, ConsonantFeatures> = mapOf(
    // Plosives
    "p"  to ConsonantFeatures(Manner.PLOSIVE,     Place.BILABIAL,     voiced = false),
    "b"  to ConsonantFeatures(Manner.PLOSIVE,     Place.BILABIAL,     voiced = true),
    "t"  to ConsonantFeatures(Manner.PLOSIVE,     Place.ALVEOLAR,     voiced = false),
    "d"  to ConsonantFeatures(Manner.PLOSIVE,     Place.ALVEOLAR,     voiced = true),
    "k"  to ConsonantFeatures(Manner.PLOSIVE,     Place.VELAR,        voiced = false),
    "ɡ"  to ConsonantFeatures(Manner.PLOSIVE,     Place.VELAR,        voiced = true),
    // Fricatives
    "f"  to ConsonantFeatures(Manner.FRICATIVE,   Place.LABIODENTAL,  voiced = false),
    "v"  to ConsonantFeatures(Manner.FRICATIVE,   Place.LABIODENTAL,  voiced = true),
    "θ"  to ConsonantFeatures(Manner.FRICATIVE,   Place.DENTAL,       voiced = false),
    "ð"  to ConsonantFeatures(Manner.FRICATIVE,   Place.DENTAL,       voiced = true),
    "s"  to ConsonantFeatures(Manner.FRICATIVE,   Place.ALVEOLAR,     voiced = false),
    "z"  to ConsonantFeatures(Manner.FRICATIVE,   Place.ALVEOLAR,     voiced = true),
    "ʃ"  to ConsonantFeatures(Manner.FRICATIVE,   Place.POSTALVEOLAR, voiced = false),
    "ʒ"  to ConsonantFeatures(Manner.FRICATIVE,   Place.POSTALVEOLAR, voiced = true),
    "h"  to ConsonantFeatures(Manner.FRICATIVE,   Place.GLOTTAL,      voiced = false),
    // Affricates
    "tʃ" to ConsonantFeatures(Manner.AFFRICATE,   Place.POSTALVEOLAR, voiced = false),
    "dʒ" to ConsonantFeatures(Manner.AFFRICATE,   Place.POSTALVEOLAR, voiced = true),
    // Nasals
    "m"  to ConsonantFeatures(Manner.NASAL,       Place.BILABIAL,     voiced = true),
    "n"  to ConsonantFeatures(Manner.NASAL,       Place.ALVEOLAR,     voiced = true),
    "ŋ"  to ConsonantFeatures(Manner.NASAL,       Place.VELAR,        voiced = true),
    // Lateral / Rhotic / Glide
    "l"  to ConsonantFeatures(Manner.LATERAL,     Place.ALVEOLAR,     voiced = true),
    "r"  to ConsonantFeatures(Manner.RHOTIC,      Place.ALVEOLAR,     voiced = true),
    "w"  to ConsonantFeatures(Manner.GLIDE,       Place.BILABIAL,     voiced = true),
    "j"  to ConsonantFeatures(Manner.GLIDE,       Place.PALATAL,      voiced = true),
)

private val VOWEL_TABLE: Map<String, VowelFeatures> = mapOf(
    // High front
    "iː" to VowelFeatures(VowelHeight.HIGH, VowelBackness.FRONT,   rounded = false, tense = true),
    "ɪ"  to VowelFeatures(VowelHeight.HIGH, VowelBackness.FRONT,   rounded = false, tense = false),
    // High back
    "uː" to VowelFeatures(VowelHeight.HIGH, VowelBackness.BACK,    rounded = true,  tense = true),
    "ʊ"  to VowelFeatures(VowelHeight.HIGH, VowelBackness.BACK,    rounded = true,  tense = false),
    // Mid front
    "eɪ" to VowelFeatures(VowelHeight.MID,  VowelBackness.FRONT,   rounded = false, tense = true,  diphthong = true),
    "e"  to VowelFeatures(VowelHeight.MID,  VowelBackness.FRONT,   rounded = false, tense = true),
    "ɛ"  to VowelFeatures(VowelHeight.MID,  VowelBackness.FRONT,   rounded = false, tense = false),
    // Mid central
    "ə"  to VowelFeatures(VowelHeight.MID,  VowelBackness.CENTRAL, rounded = false, tense = false),
    "ɜː" to VowelFeatures(VowelHeight.MID,  VowelBackness.CENTRAL, rounded = false, tense = true),
    "ʌ"  to VowelFeatures(VowelHeight.MID,  VowelBackness.CENTRAL, rounded = false, tense = false),
    // Mid back
    "oʊ" to VowelFeatures(VowelHeight.MID,  VowelBackness.BACK,    rounded = true,  tense = true,  diphthong = true),
    "ɔː" to VowelFeatures(VowelHeight.MID,  VowelBackness.BACK,    rounded = true,  tense = true),
    // Low front
    "æ"  to VowelFeatures(VowelHeight.LOW,  VowelBackness.FRONT,   rounded = false, tense = false),
    // Low central/back
    "a"  to VowelFeatures(VowelHeight.LOW,  VowelBackness.CENTRAL, rounded = false, tense = false),
    "ɑː" to VowelFeatures(VowelHeight.LOW,  VowelBackness.BACK,    rounded = false, tense = true),
    // Diphthongs
    "aɪ" to VowelFeatures(VowelHeight.LOW,  VowelBackness.FRONT,   rounded = false, tense = true,  diphthong = true),
    "aʊ" to VowelFeatures(VowelHeight.LOW,  VowelBackness.BACK,    rounded = false, tense = true,  diphthong = true),
    "ɔɪ" to VowelFeatures(VowelHeight.MID,  VowelBackness.BACK,    rounded = true,  tense = true,  diphthong = true),
)

/**
 * GOPScorer — tính confusion cost và điểm GOP cho từng phoneme.
 *
 * Cost được tính từ phonetic feature distance, không hardcode
 * theo quốc tịch người dùng. Mọi ngôn ngữ mẹ đẻ đều dùng
 * cùng một formula — cost tự nhiên cao hơn khi 2 âm khác nhau
 * nhiều về acoustic properties.
 */
object GOPScorer {

    /**
     * Tính confusion cost (0–95) giữa 2 phoneme dựa trên:
     *  - Consonant: voicing + manner distance + place distance
     *  - Vowel:     height + backness + rounding + tenseness + diphthong
     *  - Cross-class (consonant vs vowel): luôn = 90
     *
     * Cost 0  → hai âm giống hệt nhau
     * Cost 15 → chỉ khác voicing (/p/↔/b/)
     * Cost 37 → khác manner và place (/θ/↔/d/)
     * Cost 90 → consonant vs vowel
     */
    fun confusionCost(expected: String, actual: String): Int {
        if (expected == actual) return 0
        val c1 = CONSONANT_TABLE[expected]; val c2 = CONSONANT_TABLE[actual]
        val v1 = VOWEL_TABLE[expected];     val v2 = VOWEL_TABLE[actual]
        return when {
            c1 != null && c2 != null -> consonantCost(c1, c2)
            v1 != null && v2 != null -> vowelCost(v1, v2)
            else                     -> 90  // consonant vs vowel — cross-class
        }
    }

    /**
     * Tính điểm GOP 0–100 cho một phoneme.
     *  - Đúng (expected == actual) → 90 + baseNoise, capped [82, 100]
     *  - Deletion (actual == null)  → 0
     *  - Sai (substitution)         → 100 − confusionCost
     *    (âm gần acoustic → điểm cao hơn, âm rất khác → điểm thấp)
     */
    fun score(
        expected: String,
        actual: String?,
        baseNoise: Int = 0
    ): Int {
        if (actual == null) return 0
        if (expected == actual) return (90 + baseNoise).coerceIn(82, 100)
        return (100 - confusionCost(expected, actual)).coerceIn(0, 100)
    }

    fun errorType(expected: String, actual: String?): ErrorType = when {
        actual == null     -> ErrorType.DELETION
        expected == actual -> ErrorType.CORRECT
        else               -> ErrorType.SUBSTITUTION
    }
}


// ─────────────────────────────────────────────
// Bước 4 — Aggregator: tổng hợp điểm
// ─────────────────────────────────────────────

object ScoreAggregator {

    /**
     * Tổng hợp PhonemeScore → WordScore.
     * Bỏ qua INSERTION khi tính trung bình (người dùng thêm âm thừa
     * không penalize nặng bằng thiếu âm).
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
     *   - accuracyScore  = avg điểm chỉ phần đã đọc (bỏ phần chưa đọc)
     *   - completenessScore = coveredPhonemes / totalRef  (thông tin, không penalize)
     *   - finalScore = accuracyScore - fluencyPenalty  (không nhân completeness)
     *
     * Full mode:
     *   - finalScore = accuracyScore × completeness - fluencyPenalty
     */
    fun aggregateSentence(
        referenceText: String,
        wordScores: List<WordScore>,
        totalReferencePhonemes: Int,
        coveredPhonemes: Int,        // số phoneme ref đã được align (≤ total)
        spokenPhonemes: Int,         // số phoneme người dùng thực sự phát ra
        fluencyPenalty: Int = 0,
        isPartial: Boolean = false
    ): SentenceScore {
        val allPhonemeScores = wordScores.flatMap { it.phonemeScores }
            .filter { it.errorType != ErrorType.INSERTION }

        // Accuracy = avg score chỉ trên phần đã align
        val accuracyScore = if (allPhonemeScores.isEmpty()) 0
                            else allPhonemeScores.sumOf { it.score } / allPhonemeScores.size

        // Completeness = bao nhiêu % câu đã được đọc
        val completenessScore = if (totalReferencePhonemes == 0) 100
                                else (coveredPhonemes * 100) / totalReferencePhonemes

        // Partial: finalScore = accuracy thuần (không nhân completeness)
        // Full:    finalScore = accuracy × completeness (penalize bỏ sót)
        val rawFinal = if (isPartial) {
            accuracyScore - fluencyPenalty
        } else {
            (accuracyScore * completenessScore / 100) - fluencyPenalty
        }
        val finalScore = rawFinal.coerceIn(0, 100)

        val errors = allPhonemeScores
            .filter { it.errorType != ErrorType.CORRECT }
            .map { ph ->
                PronunciationError(
                    phoneme     = ph.expected,
                    errorType   = ph.errorType,
                    substitutedWith = if (ph.errorType == ErrorType.SUBSTITUTION) ph.actual else null,
                    wordContext = wordScores.find { ws ->
                        ws.phonemeScores.any { it === ph }
                    }?.word ?: ""
                )
            }

        return SentenceScore(
            referenceText       = referenceText,
            wordScores          = wordScores,
            accuracyScore       = accuracyScore,
            completenessScore   = completenessScore,
            fluencyPenalty      = fluencyPenalty,
            finalScore          = finalScore,
            errors              = errors,
            isPartial           = isPartial
        )
    }
}

// ─────────────────────────────────────────────
// Class chính — PronunciationScorer
// ─────────────────────────────────────────────

/**
 * PronunciationScorer — orchestrate toàn bộ pipeline.
 *
 * Cách dùng:
 *   val scorer = PronunciationScorer()
 *
 *   // Chấm điểm khi đọc xong câu
 *   val result = scorer.score(
 *       referenceText    = "the cat sat",
 *       spokenPhonemes   = listOf("d", "ə", "k", "ɛ", "t", "s", "æ")
 *   )
 *
 *   // Chấm điểm partial (chưa đọc xong)
 *   val partial = scorer.scorePartial(
 *       referenceText  = "the cat sat",
 *       spokenPhonemes = listOf("d", "ə", "k")
 *   )
 */
class PronunciationScorer {

    /**
     * Chấm điểm đầy đủ.
     *
     * @param referenceText   Câu cần đọc ("the cat sat")
     * @param spokenPhonemes  Chuỗi IPA người dùng phát ra từ wav2vec2
     * @param fluencyPenalty  Điểm trừ do dừng nhiều (0–20, do VAD cung cấp)
     * @param noiseLevel      Nhiễu môi trường 0–5 ảnh hưởng baseline score
     */
    fun score(
        referenceText: String,
        spokenPhonemes: List<String>,
        fluencyPenalty: Int = 0,
        noiseLevel: Int = 0
    ): SentenceScore = score(
        wordPhonemes   = G2PConverter.convert(referenceText),
        spokenPhonemes = spokenPhonemes,
        fluencyPenalty = fluencyPenalty,
        noiseLevel     = noiseLevel
    )

    /**
     * Chấm điểm — caller cấp sẵn list cặp (word, IPA) thay vì dựa G2P nội bộ.
     * Dùng khi reference đã được tra cứu từ dictionary đầy đủ ở tầng trên.
     */
    fun score(
        wordPhonemes: List<Pair<String, List<String>>>,
        spokenPhonemes: List<String>,
        fluencyPenalty: Int = 0,
        noiseLevel: Int = 0
    ): SentenceScore = scoreFull(
        wordPhonemes   = wordPhonemes,
        spokenPhonemes = spokenPhonemes,
        fluencyPenalty = fluencyPenalty,
        noiseLevel     = noiseLevel,
        isPartial      = false
    )

    /**
     * Chấm điểm partial — người dùng chưa đọc hết câu.
     * completeness không penalize vào finalScore.
     * Chỉ chấm phần reference đã được phủ bởi spokenPhonemes.
     */
    fun scorePartial(
        referenceText: String,
        spokenPhonemes: List<String>,
        fluencyPenalty: Int = 0,
        noiseLevel: Int = 0
    ): SentenceScore = scorePartial(
        wordPhonemes   = G2PConverter.convert(referenceText),
        spokenPhonemes = spokenPhonemes,
        fluencyPenalty = fluencyPenalty,
        noiseLevel     = noiseLevel
    )

    /** Partial — caller cấp sẵn list cặp (word, IPA). */
    fun scorePartial(
        wordPhonemes: List<Pair<String, List<String>>>,
        spokenPhonemes: List<String>,
        fluencyPenalty: Int = 0,
        noiseLevel: Int = 0
    ): SentenceScore = scoreFull(
        wordPhonemes   = wordPhonemes,
        spokenPhonemes = spokenPhonemes,
        fluencyPenalty = fluencyPenalty,
        noiseLevel     = noiseLevel,
        isPartial      = true
    )

    // ── private core ──────────────────────────

    private fun scoreFull(
        wordPhonemes: List<Pair<String, List<String>>>,
        spokenPhonemes: List<String>,
        fluencyPenalty: Int,
        noiseLevel: Int,
        isPartial: Boolean
    ): SentenceScore {

        // referenceText hiện được derive từ list từ — tiện cho prettyPrint
        val referenceText     = wordPhonemes.joinToString(" ") { it.first }
        val referencePhonemes = wordPhonemes.flatMap { it.second }
        val totalRef          = referencePhonemes.size

        // 2. Alignment
        //    - Partial: dùng alignPartial() — chỉ align phần ref đã được phủ.
        //      Phần ref từ coveredUpto trở đi bị bỏ qua hoàn toàn (không DELETION).
        //    - Full: vẫn dùng alignPartial() nhưng iBest sẽ = m (toàn bộ ref).
        val alignResult = PhonemeAligner.alignPartial(referencePhonemes, spokenPhonemes)
        val coveredUpto = alignResult.coveredUpto  // ref[0..coveredUpto) đã được phủ

        // 3. GOP: chỉ tính điểm các pair trong phần đã align
        val allPhonemeScores = alignResult.pairs
            .filter { it.reference != null }   // bỏ pure insertion
            .map { pair ->
                val expected = pair.reference!!
                val actual   = pair.hypothesis
                PhonemeScore(
                    expected  = expected,
                    actual    = actual,
                    score     = GOPScorer.score(expected, actual, noiseLevel),
                    errorType = GOPScorer.errorType(expected, actual)
                )
            }

        // Đếm phoneme người dùng đã phát ra (không phải deletion)
        val spokenCount = allPhonemeScores.count { it.errorType != ErrorType.DELETION }

        // 4. Map phoneme scores về từng từ
        //    Chỉ xét wordPhonemes đến từ coveredUpto — từ nào chưa đọc đến bỏ qua
        val wordScores = buildWordScores(wordPhonemes, allPhonemeScores, coveredUpto)

        // 5. Aggregate
        return ScoreAggregator.aggregateSentence(
            referenceText          = referenceText,
            wordScores             = wordScores,
            totalReferencePhonemes = totalRef,
            coveredPhonemes        = coveredUpto,
            spokenPhonemes         = spokenCount,
            fluencyPenalty         = fluencyPenalty,
            isPartial              = isPartial
        )
    }

    /**
     * Phân bổ phonemeScores về đúng WordScore của từng từ.
     *
     * @param coveredUpto Số phoneme trong reference đã được align.
     *   Từ nào bắt đầu sau coveredUpto → chưa đọc → không đưa vào kết quả.
     *   Từ nào bị cắt ngang (partially covered) → đưa vào với phần đã có.
     */
    private fun buildWordScores(
        wordPhonemes: List<Pair<String, List<String>>>,
        allPhonemeScores: List<PhonemeScore>,
        coveredUpto: Int = Int.MAX_VALUE
    ): List<WordScore> {
        var cursor = 0
        val result = mutableListOf<WordScore>()

        for ((word, phonemes) in wordPhonemes) {
            val wordStart = cursor
            val wordEnd   = cursor + phonemes.size
            cursor = wordEnd

            // Từ hoàn toàn ngoài vùng đã cover → bỏ qua (chưa đọc đến)
            if (wordStart >= coveredUpto) break

            // Từ bị cắt ngang hoặc đã đủ — lấy phần đã align
            val slice = allPhonemeScores.subList(
                wordStart.coerceAtMost(allPhonemeScores.size),
                wordEnd.coerceAtMost(allPhonemeScores.size)
            )
            result.add(ScoreAggregator.aggregateWord(word, slice))
        }
        return result
    }
}

// ─────────────────────────────────────────────
// Extension — pretty-print kết quả
// ─────────────────────────────────────────────

fun SentenceScore.prettyPrint() {
    val partial = if (isPartial) " [PARTIAL]" else ""
    val coveredPct = if (wordScores.isNotEmpty()) completenessScore else 0
    println("═══════════════════════════════════")
    println("Câu: \"$referenceText\"$partial")
    if (isPartial) println("Đã đọc: $completenessScore% câu")
    println("───────────────────────────────────")
    wordScores.forEach { ws ->
        val bar = "█".repeat(ws.score / 10) + "░".repeat(10 - ws.score / 10)
        println("  \"${ws.word.padEnd(10)}\" $bar ${ws.score}/100")
        ws.phonemeScores.forEach { ps ->
            val indicator = when (ps.errorType) {
                ErrorType.CORRECT      -> "✓"
                ErrorType.SUBSTITUTION -> "✗"
                ErrorType.DELETION     -> "∅"
                ErrorType.INSERTION    -> "+"
            }
            val detail = when (ps.errorType) {
                ErrorType.SUBSTITUTION -> " → /${ps.actual}/"
                ErrorType.DELETION     -> " (bị nuốt)"
                else                   -> ""
            }
            println("    ${indicator} /${ps.expected}/  ${ps.score}/100$detail")
        }
    }
    println("───────────────────────────────────")
    println("  Accuracy    : $accuracyScore / 100")
    println("  Completeness: $completenessScore%")
    println("  Fluency −   : $fluencyPenalty")
    println("  Điểm cuối   : $finalScore / 100")
    if (errors.isNotEmpty()) {
        println("───────────────────────────────────")
        println("  Lỗi cần sửa:")
        errors.forEach { e ->
            val msg = when (e.errorType) {
                ErrorType.SUBSTITUTION -> "/${e.phoneme}/ đọc thành /${e.substitutedWith}/ (\"${e.wordContext}\")"
                ErrorType.DELETION     -> "/${e.phoneme}/ bị nuốt (\"${e.wordContext}\")"
                else                   -> "/${e.phoneme}/ thêm thừa"
            }
            println("    • $msg")
        }
    }
    println("═══════════════════════════════════")
}

// ─────────────────────────────────────────────
// Demo — main()
// ─────────────────────────────────────────────

fun main() {
    val scorer = PronunciationScorer()

    // ── Full sentence ──────────────────────────────────────────────
    println("\n【Test 1】 Đọc đủ câu — có lỗi")
    scorer.score(
        referenceText  = "the cat sat",
        spokenPhonemes = listOf("d", "ə", "k", "ɛ", "t", "s", "æ"),
        fluencyPenalty = 5
    ).prettyPrint()

    // ── Partial: đọc được "the cat", chưa đọc "sat" ───────────────
    println("\n【Test 2】 Partial — mới đọc được \"the cat\"")
    // Kỳ vọng: chỉ chấm [ð,ə, k,æ,t], bỏ qua [s,æ,t]
    // /ð/→/d/ là lỗi, còn lại đúng → accuracy ~88
    scorer.scorePartial(
        referenceText  = "the cat sat",
        spokenPhonemes = listOf("d", "ə", "k", "æ", "t")
    ).prettyPrint()

    // ── Partial: mới đọc 1 từ ─────────────────────────────────────
    println("\n【Test 3】 Partial — mới đọc \"the\"")
    scorer.scorePartial(
        referenceText  = "the cat sat",
        spokenPhonemes = listOf("ð", "ə")
    ).prettyPrint()

    // ── Partial: đọc xong hết = giống full ────────────────────────
    println("\n【Test 4】 Partial nhưng đọc hết câu (benchmark với Test 1)")
    scorer.scorePartial(
        referenceText  = "the cat sat",
        spokenPhonemes = listOf("d", "ə", "k", "ɛ", "t", "s", "æ")
    ).prettyPrint()

    // ── Full: phát âm tốt ─────────────────────────────────────────
    println("\n【Test 5】 Phát âm tốt — đủ câu")
    scorer.score(
        referenceText  = "hello world",
        spokenPhonemes = listOf("h", "ɛ", "l", "oʊ", "w", "ɜː", "l", "d")
    ).prettyPrint()

    // ── Partial: nhiều từ, dừng giữa chừng ───────────────────────
    println("\n【Test 6】 Partial — đọc 2/3 từ \"think this\" trong \"think this that\"")
    scorer.scorePartial(
        referenceText  = "think this that",
        spokenPhonemes = listOf("t", "ɪ", "ŋ", "k", "d", "ɪ", "s")
    ).prettyPrint()
}
