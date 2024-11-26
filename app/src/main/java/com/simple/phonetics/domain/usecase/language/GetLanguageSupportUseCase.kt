package com.simple.phonetics.domain.usecase.language

import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.entities.Language
import com.simple.state.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map

class GetLanguageSupportUseCase(
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(): Flow<ResultState<List<Language>>> = languageRepository.getLanguageSupportedOrDefaultAsync().map {

        ResultState.Success(it)
    }

    data class Param(val default: Boolean)
}