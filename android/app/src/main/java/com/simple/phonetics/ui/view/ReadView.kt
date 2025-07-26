package com.simple.phonetics.ui.view

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import com.google.auto.service.AutoService
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.utils.ext.handler
import com.simple.crashlytics.logCrashlytics
import com.simple.event.listenerEvent
import com.simple.event.sendEvent
import com.simple.phonetics.EventName
import com.simple.phonetics.EventName.GET_VOICE_RESPONSE
import com.simple.phonetics.EventName.START_READING_TEXT_RESPONSE
import com.simple.phonetics.Param
import com.simple.phonetics.ui.MainActivity
import com.simple.state.ResultState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Locale

// ứng dụng sẽ đọc chữ
@AutoService(MainView::class)
class ReadView : MainView {

    private val speakInitStatus = MediatorLiveData<Int>()

    override fun setup(activity: MainActivity) {

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


        val taskId = params[Param.TASK_ID].asObjectOrNull<String>()
        val phoneticCode = params[Param.PHONETIC_CODE] as String

        val locale = withContext(handler + Dispatchers.IO) {

            phoneticCode.toLocaleOrNull()
        }

        val voiceList = withContext(handler + Dispatchers.IO) {

            textToSpeech.language = locale

            List(textToSpeech.getVoice(locale = locale).size) { index -> index }
        }


        val extras = mapOf(
            Param.TASK_ID to taskId,
            Param.VOICE_LIST to voiceList
        )


        if (voiceList.isEmpty()) {

            sendEvent(GET_VOICE_RESPONSE, ResultState.Failed(RuntimeException("not support speak")))
        } else {

            sendEvent(GET_VOICE_RESPONSE, ResultState.Success(extras))
        }

        if (voiceList.isEmpty()) withContext(handler + Dispatchers.IO) {

            val message = textToSpeech.voices.groupBy { it.locale.toString() }.mapValues { it.value.size }.toList().sortedBy { it.first }.joinToString { "${it.first}-${it.second}" }
            logCrashlytics("phoneticCode:${phoneticCode} -- language:${locale.toString()} -- voice_empty:${message}", RuntimeException())
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
        val phoneticCode = params[Param.PHONETIC_CODE] as String

        val locale = withContext(handler + Dispatchers.IO) {

            phoneticCode.toLocaleOrNull()
        }

        val voiceList = withContext(handler + Dispatchers.IO) {

            textToSpeech.language = locale

            textToSpeech.getVoice(locale = locale)
        }

        val voice = voiceList.getOrNull(voiceIndex) ?: voiceList.firstOrNull()

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

    private fun TextToSpeech.getVoice(locale: Locale?): List<Voice> {

        return voices?.filter {

            it.locale == locale
        }?.mapIndexed { _, voice ->

            voice
        } ?: emptyList()
    }


    private fun String.toLocaleOrNull(): Locale? = runCatching { toLocale() }.getOrNull()

    /**
     * Converts a language code string to its corresponding Locale object.
     *
     * Note: Some codes might be custom or non-standard;
     * they are mapped based on common assumptions or provided information.
     */
    private fun String.toLocale(): Locale? = when (this) {
        // Common languages
        "US" -> Locale.US
        "UK" -> Locale.forLanguageTag("en-GB") // English (United Kingdom)
        "en" -> Locale.US // Using Locale.US for general "en" code (depending on your context)
        "ko" -> Locale.KOREA
        "ja" -> Locale.JAPAN
        "de" -> Locale.GERMANY
        "ar" -> Locale("ar") // Arabic
        "fi" -> Locale("fi") // Finnish
        "is" -> Locale("is") // Icelandic
        "km" -> Locale("km") // Khmer
        "nb" -> Locale.forLanguageTag("nb") // Norwegian (Bokmål)
        "nl" -> Locale.forLanguageTag("nl") // Dutch
        "or" -> Locale("or") // Oriya
        "ro" -> Locale("ro") // Romanian
        "sv" -> Locale("sv") // Swedish
        "sw" -> Locale("sw") // Swahili

        // French (Variants)
        "fr_FR", "fr_QC", "fr" -> Locale.FRANCE // fr_QC maps to fr_CA, but here it defaults to FRANCE

        // Chinese (Variants)
        "zh" -> Locale.CHINESE // General Chinese
        "zh_Hans" -> Locale.SIMPLIFIED_CHINESE // Simplified Chinese
        "zh_Hant" -> Locale.TRADITIONAL_CHINESE // Traditional Chinese
        "yue" -> Locale.forLanguageTag("zh-yue") // Cantonese

        // Spanish (Variants)
        "es_ES" -> Locale("es", "ES") // Spanish (Spain)
        "es_MX" -> Locale("es", "MX") // Spanish (Mexico)

        // Other language codes from your IPA list
        "fa" -> Locale("fa") // Persian (Farsi)
        "eo" -> Locale("eo") // Esperanto
        "ma" -> Locale("mr") // Marathi (guessed from 'ma')

        // Non-standard or hard-to-determine codes, returning null unless specified
        "vi", "N", "C", "S" -> Locale("vi", "VN") // Mapped to Vietnamese (Vietnam) based on user's clarification
        "tts" -> Locale.forLanguageTag("th-isan") // Isan language (often a dialect of Thai)
        "jam" -> Locale.forLanguageTag("jam") // Jamaican Creole language

        // Default case if no explicit match is found
        else -> {
            // Attempt to create Locale from standard formats (e.g., "vi", "vi_VN")
            val parts = this.split("_", "-") // Accepts both "_" and "-" as separators
            when (parts.size) {
                1 -> Locale(parts[0]) // Only language code (e.g., "vi")
                2 -> Locale(parts[0], parts[1]) // Language and country code (e.g., "vi_VN")
                else -> null // Cannot handle other formats
            }
        }
    }
}