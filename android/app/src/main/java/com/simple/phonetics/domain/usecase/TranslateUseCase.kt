package com.simple.phonetics.domain.usecase

import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.state.ResultState
import com.simple.translate.entities.TranslateResponse

class TranslateUseCase(
    private val appRepository: AppRepository
) {

    suspend fun execute(param: Param): ResultState<List<TranslateResponse>> {

        return appRepository.translate(
            languageCodeInput = param.outputLanguageCode,
            languageCodeOutput = param.inputLanguageCode,
            text = param.input.toTypedArray()
        )
    }

    class Param(
        val input: List<String>,
        val inputLanguageCode: String,
        val outputLanguageCode: String
    )
}