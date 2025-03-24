package com.simple.phonetics.ui.view

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import com.simple.phonetics.EventName
import com.simple.phonetics.EventName.GET_VOICE_RESPONSE
import com.simple.phonetics.EventName.START_READING_TEXT_RESPONSE
import com.simple.phonetics.Param
import com.simple.phonetics.entities.Language
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.utils.listenerEvent
import com.simple.phonetics.utils.sendEvent
import com.simple.state.ResultState
import kotlinx.coroutines.flow.first
import java.util.Locale

interface ListenView {

    fun setupListen(activity: MainActivity)
}

class ListenViewImpl : ListenView {

    private val speakInitStatus = MediatorLiveData<Int>()

    override fun setupListen(activity: MainActivity) {

        val textToSpeech = TextToSpeech(activity) { status ->

            speakInitStatus.postValue(status)
        }

        listenerEvent(activity.lifecycle, EventName.GET_VOICE_REQUEST) {

            getVoice(textToSpeech = textToSpeech, params = it)
        }

        listenerEvent(activity.lifecycle, EventName.START_READING_TEXT_REQUEST) {

            speakText(textToSpeech = textToSpeech, params = it)
        }

        listenerEvent(activity.lifecycle, EventName.STOP_READING_TEXT_REQUEST) {

            sendEvent(START_READING_TEXT_RESPONSE, ResultState.Failed(RuntimeException("stop speak")))
            textToSpeech.stop()
        }

        activity.lifecycle.addObserver(object : LifecycleEventObserver {

            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {

                when (event) {
                    Lifecycle.Event.ON_PAUSE -> textToSpeech.stopSpeak()
                    Lifecycle.Event.ON_DESTROY -> textToSpeech.shutdown()
                    else -> Unit
                }
            }

            private fun TextToSpeech.stopSpeak() {

                sendEvent(START_READING_TEXT_RESPONSE, ResultState.Success(""))

                stop()
            }
        })
    }

    private suspend fun getVoice(textToSpeech: TextToSpeech, params: Any) {

        val status = speakInitStatus.asFlow().first()

        if (status == TextToSpeech.ERROR || params !is Map<*, *>) {

            sendEvent(GET_VOICE_RESPONSE, ResultState.Failed(RuntimeException("not support speak")))
            return
        }

        val languageCode = params[Param.LANGUAGE_CODE] as String

        textToSpeech.setLanguage(languageCode = languageCode)

        val voiceList = textToSpeech.getVoice(languageCode = languageCode)


        if (voiceList.isEmpty()) {

            sendEvent(GET_VOICE_RESPONSE, ResultState.Failed(RuntimeException("not support speak")))
        } else {

            sendEvent(GET_VOICE_RESPONSE, ResultState.Success(voiceList.mapIndexed { index, voice -> index }))
        }
    }

    private suspend fun speakText(textToSpeech: TextToSpeech, params: Any) {

        val speak = textToSpeech
        val status = speakInitStatus.asFlow().first()

        if (status == TextToSpeech.ERROR || params !is Map<*, *>) {

            sendEvent(START_READING_TEXT_RESPONSE, ResultState.Failed(RuntimeException("not support speak")))
            return
        }

        val text = params[Param.TEXT] as String
        val voiceIndex = params[Param.VOICE_ID] as Int
        val speakSpeed = params[Param.VOICE_SPEED] as Float
        val languageCode = params[Param.LANGUAGE_CODE] as String

        speak.setLanguage(languageCode = languageCode)

        val voice = speak.getVoice(languageCode).getOrNull(voiceIndex)

        if (voice == null) {

            sendEvent(START_READING_TEXT_RESPONSE, ResultState.Failed(RuntimeException("not support speak")))
            return
        }

        speak.setVoice(voice)
        speak.setSpeechRate(speakSpeed)

        speak.setOnUtteranceProgressListener(object : UtteranceProgressListener() {

            override fun onStart(p0: String?) {

                sendEvent(START_READING_TEXT_RESPONSE, ResultState.Running(""))
            }

            override fun onDone(p0: String?) {

                sendEvent(START_READING_TEXT_RESPONSE, ResultState.Success(""))
            }

            @Deprecated("Deprecated in Java", ReplaceWith("viewModel.updateSpeakStatus(false)"))
            override fun onError(p0: String?) {

                sendEvent(START_READING_TEXT_RESPONSE, ResultState.Failed(RuntimeException(p0 ?: "")))
            }
        })

        speak.speak(text, TextToSpeech.QUEUE_FLUSH, null, "1")
    }

    private fun TextToSpeech.getVoice(languageCode: String): List<Voice> {

        return voices?.filter {

            it.locale == languageCode.toLocale()
        }?.mapIndexed { index, voice ->

            voice
        } ?: emptyList()
    }

    private fun TextToSpeech.setLanguage(languageCode: String) {

        language = languageCode.toLocale()
    }

    private fun String.toLocale() = when (this) {
        Language.EN -> Locale.US
        Language.KO -> Locale.KOREA
        Language.JA -> Locale.JAPAN
        "de" -> Locale.GERMANY
        "fr" -> Locale.FRANCE
        "zh" -> Locale.SIMPLIFIED_CHINESE
        else -> null
    }
}