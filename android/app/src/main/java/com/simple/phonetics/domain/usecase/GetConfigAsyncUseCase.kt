package com.simple.phonetics.domain.usecase

import com.simple.phonetics.domain.repositories.AppRepository
import kotlinx.coroutines.flow.Flow

class GetConfigAsyncUseCase(
    private val appRepository: AppRepository
) {

    suspend fun execute(): Flow<Map<String, String>> {

        return appRepository.getConfigsAsync()
    }
}