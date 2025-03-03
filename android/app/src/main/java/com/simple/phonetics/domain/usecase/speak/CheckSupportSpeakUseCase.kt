package com.simple.phonetics.domain.usecase.speak

import com.simple.phonetics.domain.repositories.SpeakRepository

class CheckSupportSpeakUseCase(
    private val speakRepository: SpeakRepository
) {

    suspend fun execute(param: Param): Boolean {

        return speakRepository.checkSpeak(languageCode = param.languageCode)
    }

    data class Param(val languageCode: String)
}