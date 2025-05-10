package com.simple.phonetics.domain.usecase.language

import com.simple.phonetics.domain.repositories.LanguageRepository
import kotlinx.coroutines.flow.Flow

class GetPhoneticCodeAsyncUseCase(
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(): Flow<String> {

        return languageRepository.getPhoneticCodeAsync()
    }
}