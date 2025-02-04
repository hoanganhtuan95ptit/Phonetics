package com.simple.phonetics.domain.usecase.voice

import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.ListenRepository
import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetVoiceAsyncUseCase(
    private val listenRepository: ListenRepository,
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(): Flow<ResultState<List<Int>>> {

        return languageRepository.getLanguageInputAsync().map {

            listenRepository.getVoiceListSupportAsync(it.id)
        }
    }
}