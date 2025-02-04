package com.simple.phonetics.domain.usecase.voice

import com.simple.phonetics.domain.repositories.ListenRepository
import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow

class StartListenUseCase(
    private val listenRepository: ListenRepository
) {

    suspend fun execute(param: Param): Flow<ResultState<String>> {

        return listenRepository.startSpeakText(
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