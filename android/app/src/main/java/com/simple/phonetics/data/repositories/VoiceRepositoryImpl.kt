package com.simple.phonetics.data.repositories

import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.phonetics.EventName
import com.simple.phonetics.Param
import com.simple.phonetics.domain.repositories.VoiceRepository
import com.simple.phonetics.utils.listenerEvent
import com.simple.phonetics.utils.sendEvent
import com.simple.state.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first

class VoiceRepositoryImpl : VoiceRepository {

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

                    trySend(ResultState.Success(it.data.asObjectOrNull<List<Int>>().orEmpty()))
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

    override suspend fun startSpeakText(text: String, languageCode: String, voiceId: Int, voiceSpeed: Float): Flow<ResultState<String>> = channelFlow {

        listenerEvent(EventName.SPEAK_TEXT_RESPONSE) {

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
            EventName.SPEAK_TEXT_REQUEST,
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
}