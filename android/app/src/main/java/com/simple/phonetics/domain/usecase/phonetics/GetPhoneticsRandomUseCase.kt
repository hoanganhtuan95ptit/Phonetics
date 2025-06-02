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


        val ipa = param.text.text.replace("/", "")

        val isQueryForIpa = param.text.text.isNotEmpty() && param.text.type == Text.Type.IPA


        val wordList = getWords(param = param, isQueryForIpa = isQueryForIpa, languageCode = languageCode).map {
            it.lowercase()
        }

        val list = if (isQueryForIpa) phoneticRepository.getPhonetics(ipa = ipa, textList = wordList, phoneticCode = param.phoneticsCode).filter { phonetic ->

            phonetic.ipa[param.phoneticsCode]?.any { it.contains(ipa, true) } == true
        } else {

            phoneticRepository.getPhonetics(textList = wordList, phoneticCode = param.phoneticsCode)
        }


        /**
         * nếu không có word cho ipa trên thì thêm vào bảng
         */
        if (isQueryForIpa && list.isNotEmpty()) {

            wordRepository.insertOrUpdate(resource = param.text.text.lowercase(), languageCode = languageCode, list.map { it.text.lowercase() })
        }


        return list.shuffled().subList(0, min(list.size, param.limit))
    }

    private suspend fun getWords(param: Param, isQueryForIpa: Boolean, languageCode: String): List<String> {

        val textLengthMin = if (getWordDelimiters(languageCode = languageCode).contains("")) {
            0
        } else {
            param.textLengthMin
        }

        var list = wordRepository.getRandom(
            resource = if (isQueryForIpa) param.text.text.lowercase() else param.resource.value,
            languageCode = languageCode,

            limit = 100,

            textMin = textLengthMin,
            textLimit = param.textLengthMax
        )

        if (!isQueryForIpa || list.isNotEmpty()) {

            return list
        }


        list = wordRepository.getRandom(
            resource = Word.Resource.Popular.value,
            languageCode = languageCode,

            limit = 5000,

            textMin = textLengthMin,
            textLimit = param.textLengthMax
        )

        return list
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