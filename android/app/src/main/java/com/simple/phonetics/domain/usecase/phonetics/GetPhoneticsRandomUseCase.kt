package com.simple.phonetics.domain.usecase.phonetics

import com.simple.core.utils.extentions.toJson
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.domain.repositories.WordRepository
import com.simple.phonetics.entities.Phonetic
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


        val isQueryForIpa = param.resource.startsWith("/")


        val wordList = getWords(param = param, isQueryForIpa = isQueryForIpa, languageCode = languageCode).map {
            it.lowercase()
        }


        val phoneticList = getPhonetics(param = param, isQueryForIpa = isQueryForIpa, wordList = wordList)

        /**
         * nếu không có word cho ipa trên thì thêm vào bảng
         */
        if (isQueryForIpa && phoneticList.isNotEmpty()) {

            wordRepository.insertOrUpdate(resource = param.resource, languageCode = languageCode, phoneticList.map { it.text.lowercase() })
        }

        if (phoneticList.isEmpty() || phoneticList.size < param.limit) {

            logCrashlytics("phonetic_empty_${wordList.size}", RuntimeException(param.toJson()))
        }

        return phoneticList.shuffled().subList(0, min(phoneticList.size, param.limit))
    }

    private suspend fun getWords(param: Param, isQueryForIpa: Boolean, languageCode: String): List<String> {

        val textLengthMin = if (getWordDelimiters(languageCode = languageCode).contains("")) {
            0
        } else {
            param.textLengthMin
        }

        var list = wordRepository.getRandom(
            resource = param.resource,
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

    private suspend fun getPhonetics(param: Param, isQueryForIpa: Boolean, wordList: List<String>): List<Phonetic> {

        if (!isQueryForIpa) {

            return phoneticRepository.getPhonetics(textList = wordList, phoneticCode = param.phoneticsCode)
        }


        val ipa = param.resource.replace("/", "")

        val list = phoneticRepository.getPhonetics(ipa = ipa, textList = wordList, phoneticCode = param.phoneticsCode)

        val listFilter = list.filter { phonetic ->

            phonetic.ipa.flatMap { it.value }.any { it.contains(ipa, true) }
        }

        return listFilter
    }

    data class Param(
        val limit: Int,
        val textLengthMin: Int = 2,
        val textLengthMax: Int = 10,

        val resource: String,
        val phoneticsCode: String,
    )
}