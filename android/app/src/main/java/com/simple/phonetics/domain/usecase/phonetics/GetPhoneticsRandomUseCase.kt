package com.simple.phonetics.domain.usecase.phonetics

import android.util.Log
import com.simple.core.utils.extentions.toJson
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.entities.Word
import com.simple.phonetics.utils.exts.getWordDelimiters
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

class GetPhoneticsRandomUseCase(
    private val phoneticRepository: PhoneticRepository,
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(param: Param): List<com.simple.phonetic.entities.Phonetic> {

        val languageCode = languageRepository.getLanguageInputAsync().filterNotNull().first().id

        Log.d(TAG, "execute: param=${param.toJson()}, languageCode=$languageCode     -------------------------------------->")

        val textLengthMin = if (getWordDelimiters(languageCode = languageCode).contains("")) {
            0
        } else {
            param.textLengthMin
        }

        val ipaQuery = if (param.resource.startsWith("/")) {
            ipaWrap(phoneticCode = param.phoneticsCode, ipa = param.resource.replace("/", ""))
        } else {
            null
        }

        val resource = if (param.resource.startsWith("/")) {
            param.resource
        } else {
            param.resource
        }

        Log.d(TAG, "execute: resource=$resource, ipaCode=${param.phoneticsCode}, ipaQuery=$ipaQuery, textLengthMin=$textLengthMin, textLengthMax=${param.textLengthMax}, limit=${param.limit}")


        var phoneticList = phoneticRepository.getRandomPhonetics(
            resource = resource,
            languageCode = languageCode,
            ipaCode = param.phoneticsCode,
            ipaQuery = ipaQuery,
            textMin = textLengthMin,
            textLimit = param.textLengthMax,
            limit = param.limit
        )

        Log.d(TAG, "execute: first query result size=${phoneticList.size}, texts=${phoneticList.map { it.text }}")

        if (phoneticList.isEmpty()) {

            Log.d(TAG, "execute: first query empty, fallback to Popular")

            phoneticList = phoneticRepository.getRandomPhonetics(
                resource = Word.Resource.Popular.value,
                languageCode = languageCode,
                ipaCode = param.phoneticsCode,
                ipaQuery = ipaQuery,
                textMin = textLengthMin,
                textLimit = param.textLengthMax,
                limit = param.limit
            )

            Log.d(TAG, "execute: fallback result size=${phoneticList.size}, texts=${phoneticList}")
        }


        if (phoneticList.isEmpty() || phoneticList.size < param.limit) {

            Log.d(TAG, "execute: insufficient results ${phoneticList.size}/${param.limit}")
            logCrashlytics("phonetic_empty_${phoneticList.size}", RuntimeException(param.toJson()))
        }

        Log.d(TAG, "execute: <---------------------------------------------------------------------------------")
        
        return phoneticList
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

    companion object {
        private const val TAG = "GetPhoneticsRandomUC"
    }

    data class Param(
        val limit: Int,
        val textLengthMin: Int = 2,
        val textLengthMax: Int = 10,

        val resource: String,
        val phoneticsCode: String,
    )
}