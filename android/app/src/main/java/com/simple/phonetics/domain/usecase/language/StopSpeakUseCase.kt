package com.simple.phonetics.domain.usecase.language

import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.state.ResultState

class StopSpeakUseCase(
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(): ResultState<String> {

        return languageRepository.stopSpeakText()
    }
}