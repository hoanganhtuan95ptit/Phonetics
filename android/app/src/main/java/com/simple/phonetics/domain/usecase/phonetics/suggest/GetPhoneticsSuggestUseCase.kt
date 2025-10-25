package com.simple.phonetics.domain.usecase.phonetics.suggest

import com.simple.phonetics.domain.repositories.PhoneticRepository

class GetPhoneticsSuggestUseCase(
    private val phoneticRepository: PhoneticRepository,
) {

    suspend fun execute(param: Param): List<com.simple.phonetic.entities.Phonetic> {

        return phoneticRepository.suggest(param.text)
    }

    data class Param(
        val text: String,
    )
}