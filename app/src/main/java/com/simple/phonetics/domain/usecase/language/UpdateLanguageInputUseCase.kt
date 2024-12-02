package com.simple.phonetics.domain.usecase.language

import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.entities.Language
import com.simple.state.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

class UpdateLanguageInputUseCase(
    private val languageRepository: LanguageRepository,
) {

    suspend fun execute(param: Param): Flow<ResultState<List<State>>> = channelFlow {

        val listState = arrayListOf<State>()

        listState.add(State.START)
        trySend(ResultState.Running(listState))


        listState.add(State.SYNC_PHONETICS)
        trySend(ResultState.Running(listState))

        param.language.listIpa.forEach {

            val state = languageRepository.updatePhonetic(it)

            if (state is ResultState.Failed) {

                trySend(ResultState.Failed(state.cause))
                awaitClose()
                return@channelFlow
            }
        }


        listState.add(State.SYNC_TRANSLATE)
        trySend(ResultState.Running(listState))

        runCatching {

            val languageOutput = languageRepository.getLanguageOutput()

            languageRepository.translate(param.language.id, languageOutput.id, "hello")
        }.getOrElse {

        }

        languageRepository.updateLanguageInput(param.language)


        listState.add(State.COMPLETED)
        trySend(ResultState.Success(listState))


        awaitClose()
    }

    enum class State(val value: Int) {

        START(0),

        SYNC_PHONETICS(1),
        SYNC_TRANSLATE(2),

        COMPLETED(Int.MAX_VALUE)
    }

    data class Param(
        val language: Language
    )
}