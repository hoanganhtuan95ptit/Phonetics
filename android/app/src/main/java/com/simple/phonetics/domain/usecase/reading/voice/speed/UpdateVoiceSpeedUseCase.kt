package com.simple.phonetics.domain.usecase.reading.voice.speed

import com.simple.phonetics.domain.repositories.ReadingRepository

class UpdateVoiceSpeedUseCase(
    private val readingRepository: ReadingRepository,
) {

    suspend fun execute(param: Param) {

        readingRepository.updateVoiceSpeed(voiceSpeed = param.voiceSpeed)
    }

    data class Param(
        val voiceSpeed: Float
    )
}
