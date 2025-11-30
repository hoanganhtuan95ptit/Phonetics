package com.simple.phonetics.data.repositories

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.event.listenerEvent
import com.simple.event.sendEvent
import com.simple.phonetics.EventName
import com.simple.phonetics.Param
import com.simple.phonetics.domain.repositories.SpeakRepository
import com.simple.state.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import java.util.UUID

class SpeakRepositoryImpl : SpeakRepository {

    private val init = MediatorLiveData<Unit>()

    override fun initCompleted() {

        init.postValue(Unit)
    }

    override suspend fun checkSpeak(languageCode: String): Boolean = channelFlow {

        init.asFlow().first()

        val taskId = UUID.randomUUID().toString()

        listenerEvent(EventName.CHECK_SUPPORT_SPEAK_TEXT_RESPONSE) {

            val responseMap = it.asObjectOrNull<Map<String, Any>>() ?: emptyMap()

            val id = responseMap[Param.TASK_ID].asObjectOrNull<String>()
            val isSupport = responseMap[Param.IS_SUPPORT].asObjectOrNull<Boolean>() ?: false

            if (taskId == id) trySend(isSupport)
        }

        sendEvent(
            eventName = EventName.CHECK_SUPPORT_SPEAK_TEXT_REQUEST,
            data = mapOf(
                Param.TASK_ID to taskId,
                Param.LANGUAGE_CODE to languageCode,
            )
        )

        awaitClose {

        }
    }.first()

    override fun startSpeakText(languageCode: String): Flow<ResultState<String>> = channelFlow {

        init.asFlow().first()

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
            eventName = EventName.START_SPEAK_TEXT_REQUEST,
            data = mapOf(
                Param.LANGUAGE_CODE to languageCode,
            )
        )

        awaitClose {

            sendEvent(EventName.STOP_SPEAK_TEXT_REQUEST, Unit)
        }
    }

    override suspend fun stopSpeakText(): ResultState<String> {

        init.asFlow().first()

        sendEvent(EventName.STOP_SPEAK_TEXT_REQUEST, Unit)

        return ResultState.Success("")
    }
}