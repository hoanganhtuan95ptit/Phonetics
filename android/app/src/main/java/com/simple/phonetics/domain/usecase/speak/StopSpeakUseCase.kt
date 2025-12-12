package com.simple.phonetics.domain.usecase.speak

import com.simple.phonetics.domain.repositories.SpeakRepository
import com.simple.state.ResultState
import org.koin.core.context.GlobalContext

class StopSpeakUseCase(
    private val speakRepository: SpeakRepository
) {

    suspend fun execute(): ResultState<String> {

        return speakRepository.stopSpeakText()
    }

    companion object {

        val install: StopSpeakUseCase by lazy {
            GlobalContext.get().get()
        }
    }
}