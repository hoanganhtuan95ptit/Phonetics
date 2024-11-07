package com.simple.phonetics.data.repositories

import com.simple.coreapp.utils.extentions.offerActive
import com.simple.phonetics.EventName
import com.simple.phonetics.EventName.SPEAK_TEXT_REQUEST
import com.simple.phonetics.EventName.SPEAK_TEXT_RESPONSE
import com.simple.phonetics.Param
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.entities.Language
import com.simple.phonetics.utils.listenerEvent
import com.simple.phonetics.utils.sendEvent
import com.simple.state.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import java.util.Locale

class LanguageRepositoryImpl : LanguageRepository {

    override fun getLanguageInput(): Language {

        return Language(
            Language.EN,
            "English",
            listOf(
                Ipa("UK", "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/en_UK.txt"),
                Ipa("US", "https://raw.githubusercontent.com/hoanganhtuan95ptit/ipa-dict/master/data/en_US.txt")
            )
        )
    }

    override fun getLanguageInputAsync(): Flow<Language> = channelFlow {

        offerActive(getLanguageInput())

        awaitClose()
    }

    override fun getLanguageOutput(): Language {

        return Language(
            Locale.getDefault().language,
            Locale.getDefault().displayName,
            emptyList()
        )
    }

    override fun getLanguageOutputAsync(): Flow<Language> = channelFlow {

        offerActive(getLanguageOutput())

        awaitClose()
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
}