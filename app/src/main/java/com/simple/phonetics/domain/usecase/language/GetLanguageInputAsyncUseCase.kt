package com.simple.phonetics.domain.usecase.language

import com.simple.coreapp.data.usecase.BaseUseCase
import com.simple.phonetics.domain.entities.Language
import com.simple.phonetics.domain.repositories.LanguageRepository
import kotlinx.coroutines.flow.Flow

class GetLanguageInputAsyncUseCase(
    private val languageRepository: LanguageRepository
) : BaseUseCase<Unit, Flow<Language>> {

    override suspend fun execute(param: Unit?): Flow<Language> {

        return languageRepository.getLanguageInputAsync()
    }
}