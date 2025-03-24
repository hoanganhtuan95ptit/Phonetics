package com.simple.phonetics.domain.usecase.reading

import com.simple.phonetics.domain.repositories.ReadingRepository
import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow

class StartReadingUseCase(
    private val readingRepository: ReadingRepository
) {

    suspend fun execute(param: Param): Flow<ResultState<String>> {

        return readingRepository.startReading(
            text = param.text,

            languageCode = param.languageCode,

            voiceId = param.voiceId,
            voiceSpeed = param.voiceSpeed
        )
    }

    data class Param(
        val text: String,

        val languageCode: String,

        val voiceId: Int,
        val voiceSpeed: Float
    )
}