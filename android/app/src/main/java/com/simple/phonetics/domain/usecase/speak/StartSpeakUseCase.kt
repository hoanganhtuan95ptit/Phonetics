package com.simple.phonetics.domain.usecase.speak

import com.simple.phonetics.domain.repositories.SpeakRepository
import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow

class StartSpeakUseCase(
    private val speakRepository: SpeakRepository
) {

    fun execute(param: Param): Flow<ResultState<String>> {

        return speakRepository.startSpeakText(
            languageCode = param.languageCode,
        )
    }

    data class Param(
        val languageCode: String,
    )
}