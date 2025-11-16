package com.simple.phonetics.domain.usecase.detect

import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.state.ResultState

class DetectUseCase(
    private val appRepository: AppRepository
) {

    suspend fun execute(param: Param): ResultState<String> {

        return appRepository.detectAwait(languageCodeInput = param.inputLanguageCode, languageCodeOutput = param.outputLanguageCode, path = param.path)
    }

    data class Param(
        val path: String,
        val inputLanguageCode: String,
        val outputLanguageCode: String
    )
}