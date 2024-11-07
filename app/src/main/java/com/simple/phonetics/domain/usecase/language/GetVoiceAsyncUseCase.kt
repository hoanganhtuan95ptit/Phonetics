package com.simple.phonetics.domain.usecase.language

import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetVoiceAsyncUseCase(
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(): Flow<ResultState<List<Int>>> {

        return languageRepository.getLanguageInputAsync().map {

            languageRepository.getVoiceListSupportAsync(it.id)
        }
    }
}