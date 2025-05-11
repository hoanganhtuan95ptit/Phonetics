package com.simple.phonetics.domain.usecase.translate.selected

import com.simple.phonetics.domain.repositories.AppRepository
import kotlinx.coroutines.flow.Flow

class GetTranslateSelectedAsyncUseCase(
    private val appRepository: AppRepository
) {

    suspend fun execute(): Flow<String> {

        return appRepository.getTranslateSelectedAsync()
    }

    class Param()
}