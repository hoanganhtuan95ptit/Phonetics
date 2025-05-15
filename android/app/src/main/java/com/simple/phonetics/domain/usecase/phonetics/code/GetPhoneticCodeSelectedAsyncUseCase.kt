package com.simple.phonetics.domain.usecase.phonetics.code

import com.simple.phonetics.domain.repositories.LanguageRepository
import kotlinx.coroutines.flow.Flow

class GetPhoneticCodeSelectedAsyncUseCase(
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(): Flow<String> {

        return languageRepository.getPhoneticCodeSelectedAsync()
    }
}