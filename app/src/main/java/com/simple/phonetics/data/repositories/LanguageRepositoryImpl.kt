package com.simple.phonetics.data.repositories

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.offerActive
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.EventName
import com.simple.phonetics.EventName.SPEAK_TEXT_REQUEST
import com.simple.phonetics.EventName.SPEAK_TEXT_RESPONSE
import com.simple.phonetics.Param
import com.simple.phonetics.data.api.Api
import com.simple.phonetics.data.dao.PhoneticsDao
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Phonetics
import com.simple.phonetics.utils.listenerEvent
import com.simple.phonetics.utils.sendEvent
import com.simple.state.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import java.util.Locale

class LanguageRepositoryImpl(
    private val api: Api,
    private val phoneticsDao: PhoneticsDao
) : LanguageRepository {

    private val languageInput = MediatorLiveData(
        getLanguageSupportedDefault().first { it.id == Language.EN }
    )

    override fun getLanguageInput(): Language {

        return languageInput.get()
    }

    override fun getLanguageInputAsync(): Flow<Language> {

        return languageInput.asFlow()
    }

    override fun updateLanguageInput(language: Language) {

        languageInput.postDifferentValue(language)
    }


    override fun getLanguageOutput(): Language {

        return Language(
            Locale.getDefault().language,
            Locale.getDefault().displayName,
            "",
            emptyList()
        )
    }

    override fun getLanguageOutputAsync(): Flow<Language> = channelFlow {

        offerActive(getLanguageOutput())

        awaitClose()
    }


    override fun getLanguageSupported(): List<Language> {

        return getLanguageSupportedDefault()
    }

    override fun getLanguageSupportedDefault(): List<Language> {

        return listOf(
            Language(
                id = Language.VI,
                name = "Việt Nam",
                image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/main/flags/vietnam.png",
                listIpa = listOf(
                    Ipa("Bắc", "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/vi_C.txt"),
                    Ipa("Trung", "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/vi_N.txt"),
                    Ipa("Nam", "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/vi_S.txt")
                ),
                isSupportDetect = false
            ),
            Language(
                id = Language.EN,
                name = "English",
                image = "https://raw.githubusercontent.com/hoanganhtuan95ptit/Phonetics/refs/heads/main/flags/american.png",
                listIpa = listOf(
                    Ipa("UK", "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/en_UK.txt"),
                    Ipa("US", "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/en_US.txt")
                ),
                isSupportDetect = true
            )
        )
    }


    override suspend fun startSpeakText(text: String, languageCode: String, voiceId: Int, voiceSpeed: Float): Flow<ResultState<String>> = channelFlow {

        listenerEvent(SPEAK_TEXT_RESPONSE) {

            when (it) {

                is ResultState.Start -> {

                    trySend(it)
                }

                is ResultState.Failed -> {

                    trySend(it)
                }

                is ResultState.Running<*> -> {

                    trySend(ResultState.Running(""))
                }

                is ResultState.Success<*> -> {

                    trySend(ResultState.Success(""))
                }
            }
        }

        sendEvent(
            SPEAK_TEXT_REQUEST,
            mapOf(
                Param.TEXT to text,
                Param.LANGUAGE_CODE to languageCode,

                Param.VOICE_ID to voiceId,
                Param.VOICE_SPEED to voiceSpeed
            )
        )

        awaitClose {

        }
    }

    override suspend fun stopSpeakText(): ResultState<String> {

        sendEvent(EventName.STOP_SPEAK_TEXT_REQUEST, Unit)

        return ResultState.Success("")
    }

    override suspend fun getVoiceListSupportAsync(languageCode: String): ResultState<List<Int>> = channelFlow {

        listenerEvent(EventName.GET_VOICE_RESPONSE) {

            when (it) {

                is ResultState.Start -> {

                    trySend(it)
                }

                is ResultState.Failed -> {

                    trySend(it)
                }

                is ResultState.Success<*> -> {

                    trySend(ResultState.Success(it.data as List<Int>))
                }
            }
        }

        sendEvent(
            EventName.GET_VOICE_REQUEST,
            mapOf(
                Param.LANGUAGE_CODE to languageCode,
            )
        )

        awaitClose {

        }
    }.first()


    override suspend fun updatePhonetics(phonetics: List<Phonetics>) {

        phoneticsDao.insertOrUpdateEntities(phonetics)
    }

    override suspend fun getPhoneticBySource(it: Ipa): List<Phonetics> {

        val textAndPhonetics = hashMapOf<String, Phonetics>()

        api.syncPhonetics(it.source).string().toPhonetics(textAndPhonetics, it.code)

        return textAndPhonetics.values.toList()
    }


    private fun String.toPhonetics(textAndPhonetics: HashMap<String, Phonetics>, ipaCode: String) = split("\n").mapNotNull { phonetics ->

        val split = phonetics.split("\t", ", ").mapNotNull { ipa -> ipa.trim().takeIf { it.isNotBlank() } }.toMutableList()

        if (split.isEmpty()) return@mapNotNull null


        val text = split.removeAt(0)

        val ipa = split.map {

            var ipa = it

            if (!it.startsWith("/")) ipa = "/$it"
            if (!it.endsWith("/")) ipa = "$it/"

            ipa
        }


        val item = textAndPhonetics[text] ?: Phonetics(text).apply {

            textAndPhonetics[text] = this
        }

        if (item.ipa.isEmpty() || (!item.ipa.values.flatten().containsAll(ipa) && !ipa.containsAll(item.ipa.values.flatten()))) {

            item.ipa[ipaCode] = ipa
        }

        item
    }

}