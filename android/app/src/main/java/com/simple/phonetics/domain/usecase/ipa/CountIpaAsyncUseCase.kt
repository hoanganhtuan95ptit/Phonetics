package com.simple.phonetics.domain.usecase.ipa

import com.simple.phonetics.domain.repositories.IpaRepository
import kotlinx.coroutines.flow.Flow

class CountIpaAsyncUseCase(
    private val ipaRepository: IpaRepository
) {

    suspend fun execute(param: Param): Flow<Int> {

        return ipaRepository.getCountAsync(languageCode = param.languageCode)
    }

    data class Param(val languageCode: String)
}