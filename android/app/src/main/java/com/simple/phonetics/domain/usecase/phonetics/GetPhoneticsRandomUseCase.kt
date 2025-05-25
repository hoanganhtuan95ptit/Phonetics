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

        val isQueryForIpa = param.text.text.isNotEmpty() && param.text.type == Text.Type.IPA


        val textLengthMin = if (getWordDelimiters(languageCode = languageCode).contains("")) {
            0
        } else {
            param.textLengthMin
        }

        val resource = if (isQueryForIpa) {
            Word.Resource.Popular
        } else {
            param.resource
        }

        val limitQuery = if (isQueryForIpa) {
            5000
        } else {
            100
        }

        val wordList = wordRepository.getRandom(
            resource = resource.value,
            languageCode = languageCode,

            limit = limitQuery,

            textMin = textLengthMin,
            textLimit = param.textLengthMax
        )


        val list = if (isQueryForIpa) phoneticRepository.getPhonetics(ipa = param.text.text, textList = wordList, phoneticCode = param.phoneticsCode).filter { phonetic ->

            phonetic.ipa[param.phoneticsCode]?.any { it.contains(param.text.text) } == true
        }.shuffled() else {

            phoneticRepository.getPhonetics(textList = wordList, phoneticCode = param.phoneticsCode)
        }

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