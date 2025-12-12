package com.simple.phonetics.domain.usecase.reading

import com.simple.phonetics.domain.repositories.ReadingRepository
import com.simple.state.ResultState
import org.koin.core.context.GlobalContext

class StopReadingUseCase(
    private val readingRepository: ReadingRepository
) {

    suspend fun execute(): ResultState<String> {

        return readingRepository.stopReading()
    }

    companion object {

        val install: StopReadingUseCase by lazy {
            GlobalContext.get().get()
        }
    }
}