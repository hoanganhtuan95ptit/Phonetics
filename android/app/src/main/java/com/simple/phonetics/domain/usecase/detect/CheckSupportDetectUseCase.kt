package com.simple.phonetics.domain.usecase.detect

import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow

class CheckSupportDetectUseCase(
    private val appRepository: AppRepository
) {

    suspend fun execute(param: Param): Flow<ResultState<Boolean>> {

        return appRepository.checkSupportDetectAsync(param.languageCode, param.languageCode)
    }

    data class Param(
        val languageCode: String
    )
}