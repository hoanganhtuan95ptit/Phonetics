package com.simple.phonetics.domain.usecase

import com.simple.coreapp.utils.ext.launchCollect
import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.entities.Event
import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.entities.Language
import com.simple.state.ResultState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SyncDataUseCase(
    private val appRepository: AppRepository,
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(param: Param = Param()): Flow<ResultState<List<Ipa>>> = channelFlow {

        var job: Job? = null

        combine(
            languageRepository.getLanguageInputAsync(),
            languageRepository.getLanguageOutputAsync()
        ) { languageInput, languageOutput ->

            languageInput to languageOutput
        }.launchCollect(this) {

            val languageInput = it.first
            val languageOutput = it.second

            job?.cancel()

            job = launch {

                sync(languageInput = languageInput, languageOutput = languageOutput)
            }
        }

        awaitClose {
        }
    }

    private suspend fun sync(languageInput: Language, languageOutput: Language) {

        syncEvent(languageOutputCode = languageOutput.id)
    }

    private suspend fun syncEvent(languageOutputCode: String) = runCatching {

        val events: List<Event> = appRepository.syncEvents(languageCode = languageOutputCode)

        appRepository.updateEvents(events)
    }

    data class Param(val sync: Boolean = true)
}