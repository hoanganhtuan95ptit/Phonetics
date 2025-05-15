package com.simple.phonetics.domain.usecase.language.output

import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.entities.Language
import kotlinx.coroutines.flow.Flow

class GetLanguageOutputAsyncUseCase(
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(): Flow<Language> {

        return languageRepository.getLanguageOutputAsync()
    }
}