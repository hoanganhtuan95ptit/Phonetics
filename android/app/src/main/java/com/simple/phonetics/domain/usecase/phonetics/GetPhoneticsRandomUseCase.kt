package com.simple.phonetics.domain.usecase.phonetics

import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.domain.repositories.WordRepository
import com.simple.phonetics.entities.Phonetics
import com.simple.phonetics.entities.Word
import kotlinx.coroutines.flow.first

class GetPhoneticsRandomUseCase(
    private val wordRepository: WordRepository,
    private val phoneticRepository: PhoneticRepository,
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(param: Param): List<Phonetics> {

        val languageCode = languageRepository.getLanguageInputAsync().first().id

        val list = wordRepository.getRandom(resource = param.resource.value, languageCode = languageCode, textLimit = 5, limit = param.limit)

        return phoneticRepository.getPhonetics(phonetics = list)
    }

    data class Param(
        val limit: Int,
        val resource: Word.Resource,
    )
}