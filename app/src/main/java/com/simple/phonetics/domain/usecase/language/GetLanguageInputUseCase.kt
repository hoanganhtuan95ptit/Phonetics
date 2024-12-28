package com.simple.phonetics.domain.usecase.language

import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.entities.Language

class GetLanguageInputUseCase(
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(): Language? {

        return languageRepository.getLanguageInput()
    }
}