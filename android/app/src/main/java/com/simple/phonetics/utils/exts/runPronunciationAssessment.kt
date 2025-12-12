package com.simple.phonetics.utils.exts

import android.util.Log
import com.microsoft.cognitiveservices.speech.CancellationDetails
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentConfig
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentGradingSystem
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentGranularity
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentResult
import com.microsoft.cognitiveservices.speech.PropertyId
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.simple.phonetics.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext


sealed class AssessmentState {
    data class Recognizing(val text: String) : AssessmentState()
    data class Recognized(val text: String) : AssessmentState()
    data class Error(val message: String) : AssessmentState()
    data class FinalResult(
        val accuracy: Double,
        val pronunciations: Double,
        val fluency: Double,
        val completeness: Double,
        val json: String
    ) : AssessmentState()
}

fun runPronunciationAssessmentFlow(
    subscriptionKey: String = BuildConfig.AZURE_SPEECH_REGION,
    region: String = "eastus",
    referenceText: String = "read this sentence"
) = channelFlow<AssessmentState> {

    val speechConfig = SpeechConfig.fromSubscription(subscriptionKey, region).apply {
        speechRecognitionLanguage = "en-US"

        setProperty(PropertyId.SpeechServiceConnection_InitialSilenceTimeoutMs, "15000")
        setProperty(PropertyId.SpeechServiceConnection_EndSilenceTimeoutMs, "3000")
    }

    val audioConfig = AudioConfig.fromDefaultMicrophoneInput()
    val recognizer = SpeechRecognizer(speechConfig, audioConfig)

    val pronConfig = PronunciationAssessmentConfig(
        referenceText,
        PronunciationAssessmentGradingSystem.HundredMark,
        PronunciationAssessmentGranularity.Phoneme
    )
    pronConfig.applyTo(recognizer)

    // --- STREAM SỰ KIỆN REALTIME ---
    recognizer.recognizing.addEventListener { _, e ->
        Log.d("tuanha", "recognizing: ${e.result.text}")
        trySend(AssessmentState.Recognizing(e.result.text))
    }

    recognizer.recognized.addEventListener { _, e ->
        Log.d("tuanha", "recognized: ${e.result.text}")
        trySend(AssessmentState.Recognized(e.result.text))
    }

    recognizer.canceled.addEventListener { _, e ->
        Log.d("tuanha", "canceled: "+e.errorDetails)
        trySend(AssessmentState.Error("Canceled: ${e.errorDetails}"))
    }

    // --- NHẬN MỘT LẦN recognizeOnceAsync ---
    val task = recognizer.recognizeOnceAsync()

    // chạy trong Dispatchers.IO
    val result = withContext(Dispatchers.IO) { task.get() }

    Log.d("tuanha", "runPronunciationAssessmentFlow: ${result.reason}")

    when (result.reason) {
        ResultReason.RecognizedSpeech -> {
            val paResult = PronunciationAssessmentResult.fromResult(result)

            val json = result.properties.getProperty(
                PropertyId.SpeechServiceResponse_JsonResult
            ) ?: ""

            trySend(
                AssessmentState.FinalResult(
                    accuracy = paResult.accuracyScore,
                    pronunciations = paResult.pronunciationScore,
                    fluency = paResult.fluencyScore,
                    completeness = paResult.completenessScore,
                    json = json
                )
            )
        }

        ResultReason.NoMatch -> {
            trySend(AssessmentState.Error("No speech recognized"))
        }

        ResultReason.Canceled -> {
            val cancel = CancellationDetails.fromResult(result)
            trySend(AssessmentState.Error("Canceled: ${cancel.errorDetails}"))
        }

        else -> {
            trySend(AssessmentState.Error("Other: ${result.reason}"))
        }
    }

    // cleanup khi flow bị cancel
    awaitClose {

        Log.d("tuanha", "runPronunciationAssessmentFlow: close")

        recognizer.close()
        audioConfig.close()
        speechConfig.close()
    }
}


//{
//    "Id": "9ca283b939414b8e92f218649536ade7",
//    "RecognitionStatus": "Success",
//    "Offset": 2300000,
//    "Duration": 16000000,
//    "Channel": 0,
//    "DisplayText": "Read this sentence.",
//    "SNR": 18.502283,
//    "NBest": [
//    {
//        "Confidence": 0.9504961,
//        "Lexical": "read this sentence",
//        "ITN": "read this sentence",
//        "MaskedITN": "read this sentence",
//        "Display": "Read this sentence.",
//        "PronunciationAssessment": {
//        "AccuracyScore": 78.0,
//        "FluencyScore": 96.0,
//        "CompletenessScore": 100.0,
//        "PronScore": 86.0
//    },
//        "Words": [
//        {
//            "Word": "read",
//            "Offset": 2300000,
//            "Duration": 5000000,
//            "PronunciationAssessment": {
//            "AccuracyScore": 79.0,
//            "ErrorType": "None"
//        },
//            "Syllables": [
//            {
//                "Syllable": "riyd",
//                "Grapheme": "read",
//                "PronunciationAssessment": {
//                "AccuracyScore": 77.0
//            },
//                "Offset": 2300000,
//                "Duration": 5000000
//            }
//            ],
//            "Phonemes": [
//            {
//                "Phoneme": "r",
//                "PronunciationAssessment": {
//                "AccuracyScore": 84.0
//            },
//                "Offset": 2300000,
//                "Duration": 2200000
//            },
//            {
//                "Phoneme": "iy",
//                "PronunciationAssessment": {
//                "AccuracyScore": 92.0
//            },
//                "Offset": 4600000,
//                "Duration": 900000
//            },
//            {
//                "Phoneme": "d",
//                "PronunciationAssessment": {
//                "AccuracyScore": 61.0
//            },
//                "Offset": 5600000,
//                "Duration": 1700000
//            }
//            ]
//        },
//        {
//            "Word": "this",
//            "Offset": 7800000,
//            "Duration": 3900000,
//            "PronunciationAssessment": {
//            "AccuracyScore": 91.0,
//            "ErrorType": "None"
//        },
//            "Syllables": [
//            {
//                "Syllable": "dhihs",
//                "Grapheme": "this",
//                "PronunciationAssessment": {
//                "AccuracyScore": 68.0
//            },
//                "Offset": 7800000,
//                "Duration": 3900000
//            }
//            ],
//            "Phonemes": [
//            {
//                "Phoneme": "dh",
//                "PronunciationAssessment": {
//                "AccuracyScore": 56.0
//            },
//                "Offset": 7800000,
//                "Duration": 1500000
//            },
//            {
//                "Phoneme": "ih",
//                "PronunciationAssessment": {
//                "AccuracyScore": 84.0
//            },
//                "Offset": 9400000,
//                "Duration": 900000
//            },
//            {
//                "Phoneme": "s",
//                "PronunciationAssessment": {
//                "AccuracyScore": 69.0
//            },
//                "Offset": 10400000,
//                "Duration": 1300000
//            }
//            ]
//        },
//        {
//            "Word": "sentence",
//            "Offset": 12000000,
//            "Duration": 6300000,
//            "PronunciationAssessment": {
//            "AccuracyScore": 64.0,
//            "ErrorType": "None"
//        },
//            "Syllables": [
//            {
//                "Syllable": "sehn",
//                "Grapheme": "sen",
//                "PronunciationAssessment": {
//                "AccuracyScore": 81.0
//            },
//                "Offset": 12000000,
//                "Duration": 2900000
//            },
//            {
//                "Syllable": "taxns",
//                "Grapheme": "tence",
//                "PronunciationAssessment": {
//                "AccuracyScore": 52.0
//            },
//                "Offset": 15000000,
//                "Duration": 3300000
//            }
//            ],
//            "Phonemes": [
//            {
//                "Phoneme": "s",
//                "PronunciationAssessment": {
//                "AccuracyScore": 83.0
//            },
//                "Offset": 12000000,
//                "Duration": 1400000
//            },
//            {
//                "Phoneme": "eh",
//                "PronunciationAssessment": {
//                "AccuracyScore": 72.0
//            },
//                "Offset": 13500000,
//                "Duration": 400000
//            },
//            {
//                "Phoneme": "n",
//                "PronunciationAssessment": {
//                "AccuracyScore": 83.0
//            },
//                "Offset": 14000000,
//                "Duration": 900000
//            },
//            {
//                "Phoneme": "t",
//                "PronunciationAssessment": {
//                "AccuracyScore": 82.0
//            },
//                "Offset": 15000000,
//                "Duration": 700000
//            },
//            {
//                "Phoneme": "ax",
//                "PronunciationAssessment": {
//                "AccuracyScore": 68.0
//            },
//                "Offset": 15800000,
//                "Duration": 1400000
//            },
//            {
//                "Phoneme": "n",
//                "PronunciationAssessment": {
//                "AccuracyScore": 7.0
//            },
//                "Offset": 17300000,
//                "Duration": 400000
//            },
//            {
//                "Phoneme": "s",
//                "PronunciationAssessment": {
//                "AccuracyScore": 9.0
//            },
//                "Offset": 17800000,
//                "Duration": 500000
//            }
//            ]
//        }
//        ]
//    }
//    ]
//}