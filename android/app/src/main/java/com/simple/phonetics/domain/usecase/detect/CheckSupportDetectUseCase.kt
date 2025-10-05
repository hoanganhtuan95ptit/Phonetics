package com.simple.phonetics.domain.usecase.detect

import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.state.ResultState

class CheckSupportDetectUseCase(
    private val appRepository: AppRepository
) {

    suspend fun execute(param: Param): ResultState<Boolean> {

        return appRepository.isSupportDetect(param.languageCode, param.languageCode)
    }

    data class Param(
        val languageCode: String
    )
}