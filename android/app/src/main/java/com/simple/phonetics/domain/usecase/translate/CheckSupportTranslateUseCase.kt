package com.simple.phonetics.domain.usecase.translate

import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.state.ResultState

class CheckSupportTranslateUseCase(
    private val appRepository: AppRepository
) {

    suspend fun execute(param: Param): ResultState<Boolean> {

        return appRepository.isSupportTranslate(languageCodeInput = param.inputLanguageCode, languageCodeOutput = param.outputLanguageCode)
    }

    class Param(
        val inputLanguageCode: String,
        val outputLanguageCode: String
    )
}