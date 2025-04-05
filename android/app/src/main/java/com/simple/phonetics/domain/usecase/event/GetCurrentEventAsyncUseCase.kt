package com.simple.phonetics.domain.usecase.event

import com.simple.core.utils.AppException
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.entities.Event
import com.simple.state.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.text.SimpleDateFormat
import java.util.Locale

class GetCurrentEventAsyncUseCase(
    private val appRepository: AppRepository
) {

    suspend fun execute(): Flow<ResultState<Event>> = channelFlow {

        trySend(ResultState.Start)

        val currentTime = System.currentTimeMillis()

        appRepository.getEventsAsync().launchCollect(this) {

            val event = it.firstOrNull {
                currentTime in it.start..it.end
            }

            if (event == null) {
                trySend(ResultState.Failed(AppException(code = "", message = "not found event")))
                return@launchCollect
            }

            if (event.id.equals(appRepository.getEventIdShow(), true)) {
                trySend(ResultState.Failed(AppException(code = "", message = "event is showed")))
                return@launchCollect
            }

            trySend(ResultState.Success(event))
        }

        awaitClose {
        }
    }

    private val Event.end: Long
        get() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(timeEnd).time

    private val Event.start: Long
        get() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(timeStart).time

}