package com.simple.phonetics.domain.usecase

import com.simple.coreapp.utils.ext.launchCollect
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.tasks.SyncTask
import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.entities.Language
import com.simple.state.ResultState
import com.simple.state.isSuccess
import com.simple.task.Task
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SyncDataUseCase(
    private val syncTasks: List<SyncTask>,
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(): Flow<ResultState<List<Ipa>>> = channelFlow {

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

        val param = SyncTask.Param(
            inputLanguage = languageInput,
            outputLanguage = languageOutput
        )

        syncTasks.executeSyncAllByPriority(param).firstOrNull {

            it.isSuccess()
        }
    }

    private suspend fun <Param, Result> List<Task<Param, Result>>.executeSyncAllByPriority(param: Param) = channelFlow {

        if (isEmpty()) {

            trySend(ResultState.Failed(RuntimeException("task empty")))
            awaitClose()
            return@channelFlow
        }

        val stateList = sortedByDescending {

            it.priority()
        }.map {

            it.execute(param)
        }

        trySend(ResultState.Success(stateList))

        awaitClose {
        }
    }

    data class Param(val sync: Boolean = true)
}