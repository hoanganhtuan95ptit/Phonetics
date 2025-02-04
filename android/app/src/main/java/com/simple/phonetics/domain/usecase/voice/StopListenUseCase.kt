package com.simple.phonetics.domain.usecase.voice

import com.simple.phonetics.domain.repositories.ListenRepository
import com.simple.state.ResultState

class StopListenUseCase(
    private val listenRepository: ListenRepository
) {

    suspend fun execute(): ResultState<String> {

        return listenRepository.stopSpeakText()
    }
}