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


private const val subscriptionKeyDefault = ""

fun runPronunciationAssessment(
    subscriptionKey: String = subscriptionKeyDefault, // tốt nhất lấy từ backend token endpoint
    region: String = "eastus",
    referenceText: String = "read this sentence"
) {
    // 1. Tạo cấu hình
    val speechConfig = SpeechConfig.fromSubscription(subscriptionKey, region)
    speechConfig.speechRecognitionLanguage = "en-US"

    // (tùy chỉnh timeout nếu cần)
    speechConfig.setProperty(
        PropertyId.SpeechServiceConnection_InitialSilenceTimeoutMs,
        "15000" // 15 giây - đủ thời gian người dùng bắt đầu nói
    )
    speechConfig.setProperty(
        PropertyId.SpeechServiceConnection_EndSilenceTimeoutMs,
        "3000" // 3 giây im lặng sau khi nói xong
    )

    // 2. Audio từ micro
    val audioConfig = AudioConfig.fromDefaultMicrophoneInput()

    // 3. Tạo recognizer
    val recognizer = SpeechRecognizer(speechConfig, audioConfig)

    // 4. Tạo PronunciationAssessmentConfig
    // Các option: gradingSystem (HundredMark / FivePoint), granularity (Word/Phoneme), enableMiscue
    val pronConfig = PronunciationAssessmentConfig(referenceText, PronunciationAssessmentGradingSystem.HundredMark, PronunciationAssessmentGranularity.Phoneme)

    // Gán config vào recognizer
    pronConfig.applyTo(recognizer)

    try {
        recognizer.recognizing.addEventListener { _, e ->
            Log.d("tuanha", "Recognizing: ${e.result.text}")
        }

        recognizer.recognized.addEventListener { _, e ->
            Log.d("tuanha", "Recognized: ${e.result.text}")
        }

        recognizer.canceled.addEventListener { _, e ->
            Log.e("tuanha", "Canceled: ${e.errorDetails}")
        }

        // 5. Thực hiện 1 lần nhận (hoặc startContinuousRecognitionAsync cho continuous)
        val task = recognizer.recognizeOnceAsync()
        val result = task.get() // gọi trong worker thread / coroutine

        Log.d("tuanha", "runPronunciationAssessment: ${result.toString()}")
        when (result.reason) {
            ResultReason.RecognizedSpeech -> {
                // Lấy kết quả dưới dạng object SDK
                val paResult = PronunciationAssessmentResult.fromResult(result)
                val accuracy = paResult.accuracyScore
                val fluency = paResult.fluencyScore
                val completeness = paResult.completenessScore
                val pronunciation = paResult.pronunciationScore
                // Nếu cần phoneme/word detail, lấy JSON raw:
                val json = result.properties.getProperty(PropertyId.SpeechServiceResponse_JsonResult)
                // xử lý / hiển thị
                println("Accuracy=$accuracy Pronunciation=$pronunciation Fluency=$fluency")
                println("JSON detail: $json")
            }

            ResultReason.NoMatch -> {
                println("No speech recognized.")
            }

            ResultReason.Canceled -> {
                val cancel = CancellationDetails.fromResult(result)
                println("Canceled: ${cancel.errorDetails}")
            }

            else -> {
                println("Other result: ${result.reason}")
            }
        }
    } finally {
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