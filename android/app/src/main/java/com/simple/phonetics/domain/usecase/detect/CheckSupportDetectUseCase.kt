package com.simple.phonetics.domain.usecase.detect

import com.simple.phonetics.domain.repositories.AppRepository

class CheckSupportDetectUseCase(
    private val appRepository: AppRepository
) {

    suspend fun execute(param: Param): Boolean {

        return appRepository.checkDetect(param.languageCode, param.languageCode)
    }

    data class Param(
        val languageCode: String
    )
}