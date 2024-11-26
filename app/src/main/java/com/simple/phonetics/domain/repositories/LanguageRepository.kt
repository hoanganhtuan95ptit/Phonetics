package com.simple.phonetics.domain.repositories

import com.simple.phonetics.entities.Ipa
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Phonetics
import com.simple.state.ResultState
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


    suspend fun updatePhonetics(phonetics: List<Phonetics>)

    suspend fun getPhoneticBySource(it: Ipa): List<Phonetics>
}