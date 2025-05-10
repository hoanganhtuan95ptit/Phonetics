package com.simple.phonetics.domain.usecase.reading

import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.ReadingRepository
import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow

class StartReadingUseCase(
    private val readingRepository: ReadingRepository,
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(param: Param): Flow<ResultState<String>> {

        return readingRepository.startReading(
            text = param.text,

            phoneticCode = languageRepository.getPhoneticCode(),

            voiceId = param.voiceId,
            voiceSpeed = param.voiceSpeed
        )
    }

    data class Param(
        val text: String,

        val voiceId: Int,

        val voiceSpeed: Float
    )
}