package com.simple.phonetics.domain.usecase.reading

import com.simple.phonetics.domain.repositories.ReadingRepository
import com.simple.state.ResultState

class StopReadingUseCase(
    private val readingRepository: ReadingRepository
) {

    suspend fun execute(): ResultState<String> {

        return readingRepository.stopReading()
    }
}