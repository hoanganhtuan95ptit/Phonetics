package com.simple.phonetics.data.repositories

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import com.simple.core.utils.extentions.toJson
import com.simple.core.utils.extentions.toObject
import com.simple.coreapp.utils.extentions.offerActive
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.phonetics.DEFAULT_LANGUAGE
import com.simple.phonetics.EventName
import com.simple.phonetics.EventName.SPEAK_TEXT_REQUEST
import com.simple.phonetics.EventName.SPEAK_TEXT_RESPONSE
import com.simple.phonetics.Param
import com.simple.phonetics.data.api.Api
import com.simple.phonetics.data.cache.AppCache
import com.simple.phonetics.data.dao.PhoneticsDao
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Phonetics
import com.simple.phonetics.utils.listenerEvent
import com.simple.phonetics.utils.sendEvent
import com.simple.state.ResultState
import com.simple.task.executeSyncByPriority
import com.simple.translate.data.tasks.TranslateTask
import com.simple.translate.entities.TranslateRequest
import com.simple.translate.entities.TranslateResponse
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.util.Locale
import kotlin.collections.set

class LanguageRepositoryImpl(
    private val api: Api,
    private val appCache: AppCache,
    private val phoneticsDao: PhoneticsDao,
    private val listTranslateTask: List<TranslateTask>
) : LanguageRepository {

    private val languageList = MediatorLiveData(DEFAULT_LANGUAGE)


    private val languageInput by lazy {

        val data = appCache.getData("language_input", "")

        MediatorLiveData(
            if (data.isBlank()) null else data.toObject<Language>()
        )
    }

    override fun getLanguageInput(): Language? {

        return languageInput.value
    }

    override fun getLanguageInputAsync(): Flow<Language> {

        return languageInput.asFlow().filterNotNull()
    }

    override fun updateLanguageInput(language: Language) {

        appCache.setData("language_input", language.toJson())

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

    override suspend fun syncLanguageSupport(languageCode: String): List<Language> {

        val list = api.getLanguageSupport(languageCode = languageCode)

        languageList.postValue(list)

        return list
    }

    override suspend fun getLanguageSupportedOrDefaultAsync(): Flow<List<Language>> {

        return languageList.asFlow()
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

    override suspend fun translate(languageCodeInput: String, languageCodeOutput: String, vararg text: String): ResultState<List<TranslateResponse>> {

        val input = text.map {

            TranslateRequest(
                text = it,
                languageCode = languageCodeInput
            )
        }

        val translateState = listTranslateTask.executeSyncByPriority(TranslateTask.Param(input = input, outputCode = languageCodeOutput))

        return translateState
    }
}