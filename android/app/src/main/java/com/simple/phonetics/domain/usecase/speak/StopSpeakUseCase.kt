package com.simple.phonetics.domain.usecase.speak

import com.simple.phonetics.domain.repositories.SpeakRepository
import com.simple.state.ResultState

class StopSpeakUseCase(
    private val speakRepository: SpeakRepository
) {

    suspend fun execute(): ResultState<String> {

        return speakRepository.stopSpeakText()
    }
}