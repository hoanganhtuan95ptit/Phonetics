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
import com.simple.phonetics.EventName.SPEAK_TEXT_RESPONSE
import com.simple.phonetics.Param
import com.simple.phonetics.entities.Language
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.utils.listenerEvent
import com.simple.phonetics.utils.sendEvent
import com.simple.state.ResultState
import kotlinx.coroutines.flow.first
import java.util.Locale

interface SpeakView {

    fun setupSpeak(activity: MainActivity)
}

class SpeakViewImpl : SpeakView {

    private val speakInitStatus = MediatorLiveData<Int>()

    override fun setupSpeak(activity: MainActivity) {

        val textToSpeech = TextToSpeech(activity) { status ->

            speakInitStatus.postValue(status)
        }

        listenerEvent(activity.lifecycle, EventName.GET_VOICE_REQUEST) {

            getVoice(textToSpeech = textToSpeech, params = it)
        }

        listenerEvent(activity.lifecycle, EventName.SPEAK_TEXT_REQUEST) {

            speakText(textToSpeech = textToSpeech, params = it)
        }

        listenerEvent(activity.lifecycle, EventName.STOP_SPEAK_TEXT_REQUEST) {

            sendEvent(SPEAK_TEXT_RESPONSE, ResultState.Failed(RuntimeException("stop speak")))
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

                sendEvent(SPEAK_TEXT_RESPONSE, ResultState.Success(""))

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

            sendEvent(SPEAK_TEXT_RESPONSE, ResultState.Failed(RuntimeException("not support speak")))
            return
        }

        val text = params[Param.TEXT] as String
        val voiceIndex = params[Param.VOICE_ID] as Int
        val speakSpeed = params[Param.VOICE_SPEED] as Float
        val languageCode = params[Param.LANGUAGE_CODE] as String

        speak.setLanguage(languageCode = languageCode)

        val voice = speak.getVoice(languageCode).getOrNull(voiceIndex)

        if (voice == null) {

            sendEvent(SPEAK_TEXT_RESPONSE, ResultState.Failed(RuntimeException("not support speak")))
            return
        }

        speak.setVoice(voice)
        speak.setSpeechRate(speakSpeed)

        speak.setOnUtteranceProgressListener(object : UtteranceProgressListener() {

            override fun onStart(p0: String?) {

                sendEvent(SPEAK_TEXT_RESPONSE, ResultState.Running(""))
            }

            override fun onDone(p0: String?) {

                sendEvent(SPEAK_TEXT_RESPONSE, ResultState.Success(""))
            }

            @Deprecated("Deprecated in Java", ReplaceWith("viewModel.updateSpeakStatus(false)"))
            override fun onError(p0: String?) {

                sendEvent(SPEAK_TEXT_RESPONSE, ResultState.Failed(RuntimeException(p0 ?: "")))
            }
        })

        speak.speak(text, TextToSpeech.QUEUE_FLUSH, null, "1")
    }

    private fun TextToSpeech.getVoice(languageCode: String): List<Voice> {

        return when (languageCode) {
            Language.EN -> Locale.US
            else -> null
        }.let { locale ->

            voices?.filter { it.locale == locale }?.mapIndexed { index, voice -> voice } ?: emptyList()
        }
    }

    private fun TextToSpeech.setLanguage(languageCode: String) {

        when (languageCode) {
            Language.EN -> Locale.US
            else -> null
        }?.let {

            language = it
        }
    }

}