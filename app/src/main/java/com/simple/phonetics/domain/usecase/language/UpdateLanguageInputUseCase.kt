package com.simple.phonetics.domain.usecase.language

import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Phonetics
import com.simple.state.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

class UpdateLanguageInputUseCase(
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(param: Param): Flow<ResultState<State>> = channelFlow {

        trySend(ResultState.Start)


        val phonetics = kotlin.runCatching {

            getPhonetics(param.language)
        }.getOrElse {

            trySend(ResultState.Failed(it))
            awaitClose()
            return@channelFlow
        }

        languageRepository.updatePhonetics(phonetics)


        languageRepository.updateLanguageInput(param.language)

        trySend(ResultState.Success(State.COMPLETED))


        awaitClose()
    }

    private suspend fun getPhonetics(language: Language) = language.listIpa.flatMap {

        languageRepository.getPhoneticBySource(it)
    }.groupBy {

        it.text
    }.mapValues {

        val phonetics = Phonetics(it.key)

        it.value.map { phonetics.ipa.putAll(it.ipa) }

        phonetics
    }.values.toList()

    enum class State {

        START,
        SYNC_PHONETICS,
        COMPLETED
    }

    data class Param(
        val language: Language
    )
}