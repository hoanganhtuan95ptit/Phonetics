package com.simple.phonetics.domain.usecase.reading.voice.speed

import com.simple.phonetics.domain.repositories.ReadingRepository
import kotlinx.coroutines.flow.Flow

class GetVoiceSpeedAsyncUseCase(
    private val readingRepository: ReadingRepository
) {

    suspend fun execute(): Flow<Float> {

        return readingRepository.getVoiceSpeedAsync()
    }

    class Param()
}