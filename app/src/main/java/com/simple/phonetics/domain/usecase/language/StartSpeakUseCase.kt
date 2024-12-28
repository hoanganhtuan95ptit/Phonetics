package com.simple.phonetics.domain.usecase.language

import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow

class StartSpeakUseCase(
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(param: Param): Flow<ResultState<String>> {

        return languageRepository.startSpeakText(
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