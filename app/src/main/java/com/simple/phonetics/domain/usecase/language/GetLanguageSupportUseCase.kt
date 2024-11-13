package com.simple.phonetics.domain.usecase.language

import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.entities.Language
import com.simple.state.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

class GetLanguageSupportUseCase(
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(): Flow<ResultState<List<Language>>> = channelFlow {

        runCatching {

            languageRepository.getLanguageSupported()
        }.getOrElse {

            languageRepository.getLanguageSupportedDefault()
        }.let {

            trySend(ResultState.Success(it))
        }

        awaitClose()
    }
}