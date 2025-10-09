package com.simple.phonetics.domain.usecase.translate

import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow

class CheckSupportTranslateUseCase(
    private val appRepository: AppRepository
) {

    suspend fun execute(param: Param): Flow<ResultState<Boolean>> {

        return appRepository.checkSupportTranslateAsync(languageCodeInput = param.inputLanguageCode, languageCodeOutput = param.outputLanguageCode)
    }

    class Param(
        val inputLanguageCode: String,
        val outputLanguageCode: String
    )
}