package com.simple.phonetics.domain.usecase.phonetics

import com.simple.phonetics.domain.repositories.LanguageRepository
import kotlinx.coroutines.flow.Flow

class GetPhoneticCodeAsyncUseCase(
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(): Flow<String> {

        return languageRepository.getPhoneticCodeAsync()
    }
}