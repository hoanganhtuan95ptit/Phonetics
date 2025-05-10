package com.simple.phonetics.domain.usecase.phonetics

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
