package com.simple.phonetics.domain.usecase.language

import com.simple.phonetics.domain.repositories.LanguageRepository

class UpdatePhoneticCodeUseCase(
    private val languageRepository: LanguageRepository,
) {

    suspend fun execute(param: Param) {

        languageRepository.updatePhoneticCode(param.phoneticCode)
    }

    data class Param(
        val phoneticCode: String
    )
}
