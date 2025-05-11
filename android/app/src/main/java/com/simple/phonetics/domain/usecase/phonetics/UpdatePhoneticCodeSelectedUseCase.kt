package com.simple.phonetics.domain.usecase.phonetics

import com.simple.phonetics.domain.repositories.LanguageRepository

class UpdatePhoneticCodeSelectedUseCase(
    private val languageRepository: LanguageRepository,
) {

    suspend fun execute(param: Param) {

        languageRepository.updatePhoneticCodeSelected(param.phoneticCode)
    }

    data class Param(
        val phoneticCode: String
    )
}
