package com.simple.phonetics.domain.usecase.reading

import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.ReadingRepository
import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CheckSupportReadingAsyncUseCase(
    private val readingRepository: ReadingRepository,
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(): Flow<Boolean> {

        return languageRepository.getPhoneticCodeAsync().map {

            val state = readingRepository.getSupportedVoices(it)

            state is ResultState.Success && state.data.isNotEmpty()
        }
    }

    class Param()
}