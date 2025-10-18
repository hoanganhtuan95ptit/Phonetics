package com.simple.phonetics.domain.usecase

import com.simple.coreapp.utils.ext.launchCollect
import com.simple.ipa.entities.Ipa
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.tasks.SyncTask
import com.simple.phonetics.entities.Language
import com.simple.state.ResultState
import com.simple.state.isSuccess
import com.simple.task.Task
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull

class SyncDataUseCase(
    private val syncTasks: List<SyncTask>,
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(): Flow<ResultState<List<Ipa>>> = channelFlow {

        val languageInputFlow = channelFlow<Language?> {

            trySend(languageRepository.getLanguageInput())

            languageRepository.getLanguageInputAsync().launchCollect(this) {

                trySend(it)
            }

            awaitClose {
            }
        }.distinctUntilChanged()

        val languageOutputFlow = channelFlow<Language?> {

            languageRepository.getLanguageOutputAsync().launchCollect(this) {

                trySend(it)
            }

            awaitClose {
            }
        }.distinctUntilChanged()

        combine(
            languageInputFlow,
            languageOutputFlow
        ) { languageInput, languageOutput ->

            languageInput to languageOutput
        }.launchCollect(this) {

            sync()
        }

        awaitClose {
        }
    }

    private suspend fun sync() {

        syncTasks.executeSyncAllByPriority(SyncTask.Param()).firstOrNull {

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