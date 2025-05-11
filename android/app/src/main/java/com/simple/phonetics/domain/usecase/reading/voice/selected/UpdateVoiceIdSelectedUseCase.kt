package com.simple.phonetics.domain.usecase.reading.voice.selected

import com.simple.phonetics.domain.repositories.ReadingRepository

class UpdateVoiceIdSelectedUseCase(
    private val readingRepository: ReadingRepository,
) {

    suspend fun execute(param: Param) {

        readingRepository.updateVoiceIdSelected(voiceId = param.voiceId)
    }

    data class Param(
        val voiceId: Int
    )
}
