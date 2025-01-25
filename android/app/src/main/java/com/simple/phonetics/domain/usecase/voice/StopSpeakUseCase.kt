package com.simple.phonetics.domain.usecase.voice

import com.simple.phonetics.domain.repositories.VoiceRepository
import com.simple.state.ResultState

class StopSpeakUseCase(
    private val voiceRepository: VoiceRepository
) {

    suspend fun execute(): ResultState<String> {

        return voiceRepository.stopSpeakText()
    }
}