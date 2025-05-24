package com.simple.phonetics.domain.usecase.phonetics

import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.domain.repositories.WordRepository
import com.simple.phonetics.entities.Phonetic
import com.simple.phonetics.entities.Text
import com.simple.phonetics.entities.Word
import com.simple.phonetics.utils.exts.getWordDelimiters
import kotlinx.coroutines.flow.first
import kotlin.math.min

class GetPhoneticsRandomUseCase(
    private val wordRepository: WordRepository,
    private val phoneticRepository: PhoneticRepository,
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(param: Param): List<Phonetic> {

        val languageCode = languageRepository.getLanguageInputAsync().first().id

        val phoneticList = if (param.text.text.isEmpty()) {

            getPhoneticFromResource(param = param, languageCode = languageCode)
        } else {

            phoneticRepository.random(param.text, phoneticCode = param.phoneticsCode, limit = param.limit)
        }

        return phoneticList
    }

    private suspend fun getPhoneticFromResource(param: Param, languageCode: String): List<Phonetic> {

        val textLengthMin = if (getWordDelimiters(languageCode = languageCode).contains("")) {
            0
        } else {
            param.textLengthMin
        }

        val wordList = wordRepository.getRandom(
            resource = param.resource.value,
            languageCode = languageCode,

            limit = 100,

            textMin = textLengthMin,
            textLimit = param.textLengthMax
        )

        val list = phoneticRepository.getPhonetics(
            textList = wordList,
            phoneticCode = param.phoneticsCode
        )

        return list.subList(0, min(list.size, param.limit))
    }

    data class Param(
        val limit: Int,
        val textLengthMin: Int = 2,
        val textLengthMax: Int = 10,

        val text: Text,
        val resource: Word.Resource,
        val phoneticsCode: String,
    )
}