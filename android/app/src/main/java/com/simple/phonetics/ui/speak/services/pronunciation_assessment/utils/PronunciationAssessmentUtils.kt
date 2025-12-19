package com.simple.phonetics.ui.speak.services.pronunciation_assessment.utils

import com.microsoft.cognitiveservices.speech.PronunciationAssessmentConfig
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentGradingSystem
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentGranularity
import com.microsoft.cognitiveservices.speech.PropertyId
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.simple.core.utils.AppException
import com.simple.phonetics.BuildConfig
import com.simple.state.ResultState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

object PronunciationAssessmentUtils {

    fun recodeAsync(subscriptionKey: String = BuildConfig.AZURE_SPEECH_REGION, region: String = "eastus", referenceText: String = "") = channelFlow {

        trySend(ResultState.Running(""))


        val speechConfig = SpeechConfig.fromSubscription(subscriptionKey, region).apply {
            speechRecognitionLanguage = "en-US"

            setProperty(PropertyId.SpeechServiceConnection_InitialSilenceTimeoutMs, "3000")
            setProperty(PropertyId.SpeechServiceConnection_EndSilenceTimeoutMs, "1000")
        }

        val audioConfig = AudioConfig.fromDefaultMicrophoneInput()
        val recognizer = SpeechRecognizer(speechConfig, audioConfig)

        val pronConfig = PronunciationAssessmentConfig(
            referenceText,
            PronunciationAssessmentGradingSystem.HundredMark,
            PronunciationAssessmentGranularity.Phoneme,
        )
        pronConfig.applyTo(recognizer)


        // --- STREAM SỰ KIỆN REALTIME ---
        recognizer.recognizing.addEventListener { _, e ->
            trySend(ResultState.Running(e.result.text + "  --  " + referenceText))
        }

        // --- NHẬN MỘT LẦN recognizeOnceAsync ---
        val task = recognizer.recognizeOnceAsync()

        // chạy trong Dispatchers.IO
        launch(Dispatchers.IO) {

            val result = task.get()

            when (result.reason) {

                ResultReason.RecognizedSpeech -> {
                    trySend(ResultState.Success(result.properties.getProperty(PropertyId.SpeechServiceResponse_JsonResult).orEmpty()))
                }

                ResultReason.NoMatch -> {
                    trySend(ResultState.Failed(AppException(code = "", message = "NoMatch")))
                }

                ResultReason.Canceled -> {
                    trySend(ResultState.Failed(AppException(code = "", message = "Canceled")))
                }

                else -> {
                    trySend(ResultState.Failed(AppException(code = "", message = "Other")))
                }
            }
        }

        // cleanup khi flow bị cancel
        awaitClose {

            recognizer.close()
            audioConfig.close()
            speechConfig.close()
        }
    }
}