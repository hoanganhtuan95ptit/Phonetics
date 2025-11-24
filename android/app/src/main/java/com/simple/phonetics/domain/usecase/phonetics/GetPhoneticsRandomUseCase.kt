package com.simple.phonetics.domain.usecase.phonetics

import com.simple.core.utils.extentions.toJson
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.domain.repositories.WordRepository
import com.simple.phonetics.entities.Word
import com.simple.phonetics.utils.exts.getWordDelimiters
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlin.math.min

class GetPhoneticsRandomUseCase(
    private val wordRepository: WordRepository,
    private val phoneticRepository: PhoneticRepository,
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(param: Param): List<com.simple.phonetic.entities.Phonetic> {

        val languageCode = languageRepository.getLanguageInputAsync().filterNotNull().first().id



        val wordList = getWords(param = param, languageCode = languageCode)


        val phoneticList = if (!param.resource.startsWith("/")) {

            phoneticRepository.getPhonetic(textList = wordList, phoneticCode = param.phoneticsCode)
        } else param.resource.replace("/", "").let {

            phoneticRepository.getPhonetic(ipaQuery = ipaWrap(phoneticCode = param.phoneticsCode, ipa = it), textList = wordList, phoneticCode = param.phoneticsCode)
        }


        if (phoneticList.isEmpty() || phoneticList.size < param.limit) {

            logCrashlytics("phonetic_empty_${wordList.size}", RuntimeException(param.toJson()))
        }

        return phoneticList.shuffled().subList(0, min(phoneticList.size, param.limit))
    }

    private suspend fun getWords(param: Param, languageCode: String): List<String> {

        val textLengthMin = if (getWordDelimiters(languageCode = languageCode).contains("")) {
            0
        } else {
            param.textLengthMin
        }

        var list = wordRepository.getRandom(
            resource = param.resource,
            languageCode = languageCode,

            limit = 200,

            textMin = textLengthMin,
            textLimit = param.textLengthMax
        )

        if (list.isNotEmpty()) {

            return list
        }


        list = wordRepository.getRandom(
            resource = Word.Resource.Popular.value,
            languageCode = languageCode,

            limit = 3000,

            textMin = textLengthMin,
            textLimit = param.textLengthMax
        )

        return list
    }

    private fun ipaWrap(phoneticCode: String, ipa: String): String {

        return if (ipa.equals("l", true) && phoneticCode.equals("us", true)) {
            "ɫ"
        } else if (ipa.equals("eə", true) && phoneticCode.equals("us", true)) {
            "ɛɹ"
        } else if (ipa.equals("ɜː", true) && phoneticCode.equals("us", true)) {
            "ɝ"
        } else if (ipa.equals("ʌ", true) && phoneticCode.equals("us", true)) {
            "ə"
        } else if (ipa.equals("uː", true) && phoneticCode.equals("us", true)) {
            "u"
        } else if (ipa.equals("ɔː", true) && phoneticCode.equals("us", true)) {
            "ɔ"
        } else if (ipa.equals("iː", true) && phoneticCode.equals("us", true)) {
            "i"
        } else if (ipa.equals("ɑː", true) && phoneticCode.equals("us", true)) {
            "ə"
        } else if (ipa.equals("oʊ", true) && phoneticCode.equals("uk", true)) {
            "əʊ"
        } else if (ipa.equals("g", true)) {
            "ɡ"
        } else {
            ipa
        }
    }

    data class Param(
        val limit: Int,
        val textLengthMin: Int = 2,
        val textLengthMax: Int = 10,

        val resource: String,
        val phoneticsCode: String,
    )
}