package com.simple.phonetics.ui.view

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.auto.service.AutoService
import com.simple.core.utils.AppException
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.event.listenerEvent
import com.simple.event.sendEvent
import com.simple.phonetics.EventName
import com.simple.phonetics.Param
import com.simple.phonetics.SpeakState
import com.simple.phonetics.entities.Language
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.home.view.HomeView
import com.simple.state.ResultState
import java.util.Locale

@AutoService(MainView::class)
class SpeakView : MainView {

    override fun setup(activity: MainActivity) {

        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity)

        listenerEvent(activity.lifecycle, EventName.CHECK_SUPPORT_SPEAK_TEXT_REQUEST) {

            if (it !is Map<*, *>) {

                return@listenerEvent
            }

            val taskId = it[Param.TASK_ID].asObjectOrNull<String>()
            val languageCode = it[Param.LANGUAGE_CODE].asObjectOrNull<String>()
            val languageWrap = languageCode?.languageWrap()

            val isSupport = languageWrap != null
                    && SpeechRecognizer.isRecognitionAvailable(activity)
                    && Locale(languageCode).runCatching { isO3Language }.getOrNull() != null

            val data = mapOf(
                Param.TASK_ID to taskId,
                Param.IS_SUPPORT to isSupport
            )

            sendEvent(EventName.CHECK_SUPPORT_SPEAK_TEXT_RESPONSE, data)
        }

        listenerEvent(activity.lifecycle, EventName.START_SPEAK_TEXT_REQUEST) {

            if (it !is Map<*, *>) {

                return@listenerEvent
            }

            val languageWrap = it[Param.LANGUAGE_CODE].asObjectOrNull<String>()?.languageWrap()

            if (languageWrap == null) {

                sendEvent(EventName.START_SPEAK_TEXT_RESPONSE, ResultState.Failed(AppException("")))
                return@listenerEvent
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageWrap)
            speechRecognizer?.startListening(intent)
        }

        listenerEvent(activity.lifecycle, EventName.STOP_SPEAK_TEXT_REQUEST) {

            speechRecognizer.stopListen()
        }

        activity.lifecycle.addObserver(object : LifecycleEventObserver {

            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {

                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {

//                        Log.d("tuanha", "onStateChanged: ON_PAUSE")
                        speechRecognizer.stopListen()
                    }

                    Lifecycle.Event.ON_DESTROY -> {

//                        Log.d("tuanha", "onStateChanged: ON_DESTROY")
                        speechRecognizer.stopListen()
                        speechRecognizer.cancel()
                        speechRecognizer.destroy()
                    }

                    else -> Unit
                }
            }
        })

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle) {
                // Sẵn sàng nhận dạng giọng nói

//                Log.d("tuanha", "onReadyForSpeech: ")
                sendEvent(EventName.START_SPEAK_TEXT_RESPONSE, ResultState.Running(SpeakState.READY))
            }

            override fun onBeginningOfSpeech() {
                // Người dùng bắt đầu nói

//                Log.d("tuanha", "onBeginningOfSpeech: ")
                sendEvent(EventName.START_SPEAK_TEXT_RESPONSE, ResultState.Running(SpeakState.RECORD_START))
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Cường độ âm thanh thay đổi
//                Log.d("tuanha", "onRmsChanged: ")
            }

            override fun onBufferReceived(buffer: ByteArray) {
                // Nhận dữ liệu âm thanh
//                Log.d("tuanha", "onBufferReceived: ")
            }

            override fun onEndOfSpeech() {
                // Người dùng đã ngừng nói
//                Log.d("tuanha", "onEndOfSpeech: ")
                sendEvent(EventName.START_SPEAK_TEXT_RESPONSE, ResultState.Running(SpeakState.RECORD_END))
            }

            override fun onError(error: Int) {
                // Xử lý lỗi khi nhận dạng giọng nói
//                Log.d("tuanha", "onError: $error")
                sendEvent(EventName.START_SPEAK_TEXT_RESPONSE, ResultState.Failed(AppException("$error")))
            }

            override fun onResults(results: Bundle) {
                // Kết quả nhận dạng giọng nói

                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                if (matches != null) {
//                    Log.d("tuanha", "onResults: ${matches.toList()}")
                    sendEvent(EventName.START_SPEAK_TEXT_RESPONSE, ResultState.Success(matches[0]))
                }
            }

            override fun onPartialResults(partialResults: Bundle) {
                // Kết quả nhận dạng phần nào
            }

            override fun onEvent(eventType: Int, params: Bundle) {
                // Xử lý các sự kiện đặc biệt
//                Log.d("tuanha", "onEvent: ")
            }
        })
    }

    private fun SpeechRecognizer.stopListen() {

        stopListening()
    }

    private fun String.languageWrap() = when (this) {
        Language.EN -> "en-US"
        Language.VI -> "vi-VN"
        "es" -> "es-ES"
        "fr" -> "fr-FR"
        "de" -> "de-DE"
        "zh" -> "zh-CN"
        Language.JA -> "ja-JP"
        Language.KO -> "ko-KR"
        "hi" -> "hi-IN"
        "ar" -> "ar-SA"
        else -> null
    }
}