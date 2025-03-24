package com.simple.phonetics.domain.usecase.reading

import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.ReadingRepository
import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetVoiceAsyncUseCase(
    private val readingRepository: ReadingRepository,
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(): Flow<ResultState<List<Int>>> {

        return languageRepository.getLanguageInputAsync().map {

            readingRepository.getSupportedVoices(it.id)
        }
    }
}