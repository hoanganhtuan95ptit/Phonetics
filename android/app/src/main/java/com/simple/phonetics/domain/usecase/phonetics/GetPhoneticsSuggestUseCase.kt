package com.simple.phonetics.domain.usecase.phonetics

import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.entities.Phonetic

class GetPhoneticsSuggestUseCase(
    private val phoneticRepository: PhoneticRepository,
) {

    suspend fun execute(param: Param): List<Phonetic> {

        return phoneticRepository.suggestPhonetics(param.text)
    }

    data class Param(
        val text: String,
    )
}