package com.simple.phonetics.domain.usecase

import com.simple.phonetics.domain.repositories.AppRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.context.GlobalContext

class GetConfigAsyncUseCase(
    private val appRepository: AppRepository
) {

    suspend fun execute(): Flow<Map<String, String>> {

        return appRepository.getConfigsAsync()
    }

    companion object {

        val install by lazy {
            GlobalContext.get().get<GetConfigAsyncUseCase>()
        }
    }
}