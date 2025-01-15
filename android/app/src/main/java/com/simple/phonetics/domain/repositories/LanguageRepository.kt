package com.simple.phonetics.domain.repositories

import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Phonetics
import com.simple.state.ResultState
import com.simple.translate.entities.TranslateResponse
import kotlinx.coroutines.flow.Flow

interface LanguageRepository {

    fun getLanguageInput(): Language?

    fun getLanguageInputAsync(): Flow<Language>

    fun updateLanguageInput(language: Language)


    fun getLanguageOutput(): Language

    fun getLanguageOutputAsync(): Flow<Language>


    suspend fun syncLanguageSupport(languageCode: String): List<Language>

    suspend fun getLanguageSupportedOrDefaultAsync(): Flow<List<Language>>

    suspend fun getVoiceListSupportAsync(languageCode: String): ResultState<List<Int>>

    suspend fun startSpeakText(text: String, languageCode: String, voiceId: Int, voiceSpeed: Float): Flow<ResultState<String>>

    suspend fun stopSpeakText(): ResultState<String>


    suspend fun toPhonetics(dataSplit: String, code: String): Map<String, Phonetics>

    suspend fun getPhonetics(phonetics: List<String>): List<Phonetics>

    suspend fun insertOrUpdate(phonetics: List<Phonetics>)

    suspend fun updatePhonetics(phonetics: List<Phonetics>)

    suspend fun getSourcePhonetic(it: Ipa): String

    suspend fun updatePhonetic(it: Ipa): ResultState<Unit>


    suspend fun getPhoneticBySource(it: Ipa): List<Phonetics>

    suspend fun translate(languageCodeInput: String, languageCodeOutput: String, vararg text: String): ResultState<List<TranslateResponse>>
}