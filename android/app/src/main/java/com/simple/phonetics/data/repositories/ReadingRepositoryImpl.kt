package com.simple.phonetics.data.repositories

import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.event.listenerEvent
import com.simple.event.sendEvent
import com.simple.phonetics.EventName
import com.simple.phonetics.Param
import com.simple.phonetics.data.cache.AppCache
import com.simple.phonetics.domain.repositories.ReadingRepository
import com.simple.state.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ReadingRepositoryImpl(
    private val appCache: AppCache
) : ReadingRepository {


    override suspend fun getVoiceIdSelected(): Int {

        return appCache.getData(VOICE_ID, 0)
    }

    override suspend fun getVoiceIdSelectedAsync(): Flow<Int> {

        return appCache.getDataAsync(VOICE_ID).map {

            getVoiceIdSelected()
        }.distinctUntilChanged()
    }

    override suspend fun updateVoiceIdSelected(voiceId: Int) {

        appCache.setData(VOICE_ID, voiceId)
    }


    override suspend fun getVoiceSpeed(): Float {
        return appCache.getData(VOICE_SPEED, 1f)
    }

    override suspend fun getVoiceSpeedAsync(): Flow<Float> {

        return appCache.getDataAsync(VOICE_SPEED).map {

            getVoiceSpeed()
        }.distinctUntilChanged()
    }

    override suspend fun updateVoiceSpeed(voiceSpeed: Float) {
        appCache.setData(VOICE_SPEED, voiceSpeed)
    }


    override suspend fun getSupportedVoices(phoneticCode: String): ResultState<List<Int>> = channelFlow {

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
                Param.PHONETIC_CODE to phoneticCode,
            )
        )

        awaitClose {

        }
    }.first()

    override suspend fun startReading(text: String, phoneticCode: String, voiceId: Int, voiceSpeed: Float): Flow<ResultState<String>> = channelFlow {

        listenerEvent(EventName.START_READING_TEXT_RESPONSE) {

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
            EventName.START_READING_TEXT_REQUEST,
            mapOf(
                Param.TEXT to text,
                Param.PHONETIC_CODE to phoneticCode,

                Param.VOICE_ID to voiceId,
                Param.VOICE_SPEED to voiceSpeed
            )
        )

        awaitClose {

            sendEvent(EventName.STOP_READING_TEXT_REQUEST, Unit)
        }
    }

    override suspend fun stopReading(): ResultState<String> {

        sendEvent(EventName.STOP_READING_TEXT_REQUEST, Unit)

        return ResultState.Success("")
    }

    companion object {

        private const val VOICE_ID = "voice_id"
        private const val VOICE_SPEED = "voice_speed"
    }
}