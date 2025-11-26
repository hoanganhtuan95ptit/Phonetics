package com.simple.phonetics.domain.usecase.speak

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.SpeakRepository
import com.unknown.coroutines.handler
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

private val checkSupportSpeakAsync: MutableLiveData<Boolean> = object : MutableLiveData<Boolean>() {

    private var job: Job? = null

    override fun onActive() {
        super.onActive()

        job?.cancel()

        job = refreshAsync().launchCollect(CoroutineScope(Dispatchers.Main)) {

            value = it
        }
    }

    override fun onInactive() {
        super.onInactive()

        job?.cancel()
    }

    private fun refreshAsync() = channelFlow {

        LanguageRepository.instant.getLanguageInputAsync().filterNotNull().map {

            SpeakRepository.instant.checkSpeak(it.id)
        }.collect {

            trySend(it)
        }

        awaitClose()
    }.flowOn(handler + Dispatchers.IO)
}

class CheckSupportSpeakAsyncUseCase(
    private val speakRepository: SpeakRepository,
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(): Flow<Boolean> {

        return checkSupportSpeakAsync.asFlow()
    }
}