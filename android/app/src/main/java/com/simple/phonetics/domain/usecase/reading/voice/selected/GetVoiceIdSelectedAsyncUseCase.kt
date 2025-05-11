package com.simple.phonetics.domain.usecase.reading.voice.selected

import com.simple.phonetics.domain.repositories.ReadingRepository
import kotlinx.coroutines.flow.Flow

class GetVoiceIdSelectedAsyncUseCase(
    private val readingRepository: ReadingRepository
) {

    suspend fun execute(): Flow<Int> {

        return readingRepository.getVoiceIdSelectedAsync()
    }

    class Param()
}