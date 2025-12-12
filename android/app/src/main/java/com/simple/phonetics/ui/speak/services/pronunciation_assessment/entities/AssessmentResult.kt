package com.simple.phonetics.ui.speak.services.pronunciation_assessment.entities

import androidx.annotation.Keep
import com.fasterxml.jackson.annotation.JsonProperty

@Keep
data class AssessmentResult(
    @JsonProperty("Id") val id: String,
    @JsonProperty("RecognitionStatus") val recognitionStatus: String,
    @JsonProperty("Offset") val offset: Long,
    @JsonProperty("Duration") val duration: Long,
    @JsonProperty("Channel") val channel: Int,
    @JsonProperty("DisplayText") val displayText: String,
    @JsonProperty("SNR") val snr: Double,
    @JsonProperty("NBest") val nbest: List<NBestItem>
)

@Keep
data class NBestItem(
    @JsonProperty("Confidence") val confidence: Double,
    @JsonProperty("Lexical") val lexical: String,
    @JsonProperty("ITN") val itn: String,
    @JsonProperty("MaskedITN") val maskedItn: String,
    @JsonProperty("Display") val display: String,
    @JsonProperty("PronunciationAssessment") val pronunciationAssessment: PronunciationAssessment,
    @JsonProperty("Words") val words: List<WordItem>
)

@Keep
data class PronunciationAssessment(
    @JsonProperty("AccuracyScore") val accuracyScore: Double,
    @JsonProperty("FluencyScore") val fluencyScore: Double? = null,
    @JsonProperty("CompletenessScore") val completenessScore: Double? = null,
    @JsonProperty("PronScore") val pronScore: Double? = null,
    @JsonProperty("ErrorType") val errorType: String? = null
)

@Keep
data class WordItem(
    @JsonProperty("Word") val word: String,
    @JsonProperty("Offset") val offset: Long,
    @JsonProperty("Duration") val duration: Long,
    @JsonProperty("PronunciationAssessment") val pronunciationAssessment: PronunciationAssessment,
    @JsonProperty("Syllables") val syllables: List<SyllableItem>,
    @JsonProperty("Phonemes") val phonemes: List<PhonemeItem>
)

@Keep
data class SyllableItem(
    @JsonProperty("Syllable") val syllable: String,
    @JsonProperty("Grapheme") val grapheme: String,
    @JsonProperty("PronunciationAssessment") val pronunciationAssessment: PronunciationAssessment,
    @JsonProperty("Offset") val offset: Long,
    @JsonProperty("Duration") val duration: Long
)

@Keep
data class PhonemeItem(
    @JsonProperty("Phoneme") val phoneme: String,
    @JsonProperty("PronunciationAssessment") val pronunciationAssessment: PronunciationAssessment,
    @JsonProperty("Offset") val offset: Long,
    @JsonProperty("Duration") val duration: Long
)
