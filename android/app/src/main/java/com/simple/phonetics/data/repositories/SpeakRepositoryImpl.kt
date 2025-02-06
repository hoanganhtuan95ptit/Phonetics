package com.simple.phonetics.data.repositories

import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.phonetics.EventName
import com.simple.phonetics.Param
import com.simple.phonetics.domain.repositories.SpeakRepository
import com.simple.phonetics.utils.listenerEvent
import com.simple.phonetics.utils.sendEvent
import com.simple.state.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first

class SpeakRepositoryImpl : SpeakRepository {

    override suspend fun checkSpeak(languageCode: String): Boolean = channelFlow<Boolean> {

        listenerEvent(EventName.CHECK_SUPPORT_SPEAK_TEXT_RESPONSE) {

            trySend(it.asObjectOrNull<Boolean>() ?: false)
        }

        sendEvent(
            EventName.CHECK_SUPPORT_SPEAK_TEXT_REQUEST,
            mapOf(
                Param.LANGUAGE_CODE to languageCode,
            )
        )

        awaitClose {

        }
    }.first()

    override suspend fun startSpeakText(languageCode: String): Flow<ResultState<String>> = channelFlow {

        listenerEvent(EventName.START_SPEAK_TEXT_RESPONSE) {

            when (it) {

                is ResultState.Start -> {

                    trySend(it)
                }

                is ResultState.Failed -> {

                    trySend(it)
                }

                is ResultState.Running<*> -> {

                    trySend(ResultState.Running(it.data.asObjectOrNull<String>().orEmpty()))
                }

                is ResultState.Success<*> -> {

                    trySend(ResultState.Success(it.data.asObjectOrNull<String>().orEmpty()))
                }
            }
        }

        sendEvent(
            EventName.START_SPEAK_TEXT_REQUEST,
            mapOf(
                Param.LANGUAGE_CODE to languageCode,
            )
        )

        awaitClose {

            sendEvent(EventName.STOP_SPEAK_TEXT_REQUEST, Unit)
        }
    }

    override suspend fun stopSpeakText(): ResultState<String> {

        sendEvent(EventName.STOP_SPEAK_TEXT_REQUEST, Unit)

        return ResultState.Success("")
    }
}