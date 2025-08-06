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

        val localeTagList = withContext(handler + Dispatchers.IO) {

            phoneticCode.toLocaleTagListOrEmpty()
        }

        val voiceList = withContext(handler + Dispatchers.IO) {

            List(textToSpeech.getVoice(localeTagList = localeTagList).size) { index -> index }
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

            val voices = textToSpeech.voices?.toList().orEmpty()
            if (voices.isEmpty()) return@withContext

            val message = voices.groupBy { it.locale.toString() }.mapValues { it.value.size }.toList().sortedBy { it.first }.joinToString { "${it.first}-${it.second}" }
            logCrashlytics("phoneticCode:${phoneticCode} -- language:$localeTagList -- voice_empty:${message}", RuntimeException())
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

        val localeTagList = withContext(handler + Dispatchers.IO) {

            phoneticCode.toLocaleTagListOrEmpty()
        }

        val voiceList = withContext(handler + Dispatchers.IO) {


            textToSpeech.getVoice(localeTagList = localeTagList)
        }

        val voice = voiceList.getOrNull(voiceIndex) ?: voiceList.firstOrNull()

        if (voice == null) {

            sendEvent(START_READING_TEXT_RESPONSE, ResultState.Failed(RuntimeException("not support speak")))
            return
        }

        speak.voice = voice
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

    private fun TextToSpeech.getVoice(localeTagList: List<String>): List<Voice> {

        return voices?.filter {

            val voiceLocaleTag = it.locale.toString()

            localeTagList.isNotEmpty() && localeTagList.any { tag ->
                voiceLocaleTag.equals(tag, true) || voiceLocaleTag.startsWith(tag, true)
            }
        }?.mapIndexed { _, voice ->

            voice
        } ?: emptyList()
    }

    private fun String.toLocaleTagListOrEmpty() = runCatching {

        toLocaleTagList()
    }.getOrElse {

        emptyList()
    }

    private fun String.toLocaleTagList(): List<String> = when (this) {
        // Common languages and specific variants from your list
        "US", "en" -> listOf(Locale.US.toString(), "eng_USA", "eng_USA-1", "eng_USA_default-1", "eng_USA_f00-1", "eng_USA_f02-1", "eng_USA_l03-1")
        "UK" -> listOf(Locale.forLanguageTag("en-GB").toString(), "eng_GBR", "eng_GBR-1", "eng_GBR_default-1", "eng_GBR_f00-1")
        "ko" -> listOf(Locale.KOREA.toString())
        "ja" -> listOf(Locale.JAPAN.toString(), "jpn_JPN", "jpn_JPN-1")
        "de" -> listOf(Locale.GERMANY.toString(), "deu_DEU", "deu_DEU-1", "deu_DEU_default-1", "deu_DEU_f00-1")
        "ar" -> listOf(Locale("ar").toString()) // Arabic
        "fi" -> listOf(Locale("fi").toString()) // Finnish
        "is" -> listOf(Locale("is").toString()) // Icelandic
        "km" -> listOf(Locale("km").toString()) // Khmer
        "nb" -> listOf(Locale.forLanguageTag("nb").toString()) // Norwegian (Bokmål)
        "nl" -> listOf(Locale.forLanguageTag("nl").toString()) // Dutch
        "or" -> listOf(Locale("or").toString()) // Oriya
        "ro" -> listOf(Locale("ro").toString()) // Romanian
        "sv" -> listOf(Locale("sv").toString()) // Swedish
        "sw" -> listOf(Locale("sw").toString()) // Swahili
        "th" -> listOf(Locale("th", "THA").toString(), "tha_THA", "tha_THA_default-1", "tha_THA_f00-1") // Thai

        // French (Variants)
        "fr_FR", "fr_QC", "fr" -> listOf(Locale.FRANCE.toString(), "fra_FRA-1", "fra_FRA_default-1", "fra_FRA_f00-1") // fr_QC maps to fr_CA, but here it defaults to FRANCE

        // Chinese (Variants)
        "zh" -> listOf(Locale.CHINESE.toString(), "zho_CHN", "zho_CHN-1", "zho_CHN_default-1", "zho_CHN_f00-1") // General Chinese (defaults to Simplified)
        "zh_Hans" -> listOf(Locale.SIMPLIFIED_CHINESE.toString(), "zho_CHN", "zho_CHN-1", "zho_CHN_default-1", "zho_CHN_f00-1") // Simplified Chinese
        "zh_Hant" -> listOf(Locale.TRADITIONAL_CHINESE.toString(), "zho_TWN", "zho_TWN_default-1", "zho_TWN_f00-1", "zho_HKG_default-1", "zho_HKG_f00-1") // Traditional Chinese
        "yue" -> listOf(Locale.forLanguageTag("zh-yue").toString(), "zho_HKG", "zho_HKG_default-1", "zho_HKG_f00-1") // Cantonese (often associated with HK)

        // Spanish (Variants)
        "es_ES" -> listOf(Locale("es", "ES").toString(), "spa_ESP", "spa_ESP-1", "spa_ESP_default-1", "spa_ESP_f00-1") // Spanish (Spain)
        "es_MX" -> listOf(Locale("es", "MX").toString(), "spa_MEX", "spa_MEX-1", "spa_MEX_default-1", "spa_MEX_f00-1") // Spanish (Mexico)
        "es_US" -> listOf(Locale("es", "US").toString(), "spa_USA", "spa_USA_default-1", "spa_USA_f00-1") // Spanish (USA)

        // Other language codes from your IPA list and common ones
        "fa" -> listOf(Locale("fa").toString()) // Persian (Farsi)
        "eo" -> listOf(Locale("eo").toString()) // Esperanto
        "mr" -> listOf(Locale("mr").toString()) // Marathi (confirmed from 'ma' guess)
        "hi" -> listOf(Locale("hi", "IN").toString(), "hin_IND", "hin_IND_default-1", "hin_IND_f00-1") // Hindi (India)
        "it" -> listOf(Locale.ITALY.toString(), "ita_ITA", "ita_ITA-1", "ita_ITA_default-1", "ita_ITA_f00-1") // Italian
        "pl" -> listOf(Locale("pl", "PL").toString(), "pol_POL", "pol_POL_default-1", "pol_POL_f00-1") // Polish
        "pt" -> listOf(Locale("pt").toString(), "por_BRA", "por_BRA-1", "por_BRA_default-1", "por_BRA_f00-1") // Portuguese (general, defaults to Brazil)
        "pt_BR" -> listOf(Locale("pt", "BR").toString(), "por_BRA", "por_BRA-1", "por_BRA_default-1", "por_BRA_f00-1") // Portuguese (Brazil)
        "ru" -> listOf(Locale("ru", "RU").toString(), "rus_RUS", "rus_RUS-1", "rus_RUS_default-1", "rus_RUS_f00-1") // Russian

        // Non-standard or hard-to-determine codes, returning null unless specified
        "vi", "N", "C", "S" -> listOf(Locale("vi", "VN").toString(), "vie_VNM", "vie_VNM_default-1", "vie_VNM_f00-1") // Mapped to Vietnamese (Vietnam) based on user's clarification
        "tts" -> listOf(Locale.forLanguageTag("th-isan").toString(), Locale("th", "THA").toString(), "tha_THA", "tha_THA_default-1", "tha_THA_f00-1") // Isan language (often a dialect of Thai)
        "jam" -> listOf(Locale.forLanguageTag("jam").toString()) // Jamaican Creole language

        // Default case if no explicit match is found
        else -> {
            emptyList()
        }
    }
}