package com.simple.phonetics.domain.usecase.reading

import com.simple.phonetics.domain.repositories.LanguageRepository

class UpdateVoiceSelectedUseCase(
    private val languageRepository: LanguageRepository,
) {

    suspend fun execute(param: Param) {

        languageRepository.updatePhoneticCode(param.phoneticCode)
    }

    data class Param(
        val phoneticCode: String
    )
}
