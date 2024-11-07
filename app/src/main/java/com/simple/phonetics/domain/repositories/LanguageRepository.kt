package com.simple.phonetics.domain.repositories

import com.simple.phonetics.entities.Language
import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow

interface LanguageRepository {

    fun getLanguageInput(): Language

    fun getLanguageInputAsync(): Flow<Language>


    fun getLanguageOutput(): Language

    fun getLanguageOutputAsync(): Flow<Language>


    suspend fun startSpeakText(text: String, languageCode: String, voiceId: Int, voiceSpeed: Float): Flow<ResultState<String>>

    suspend fun stopSpeakText(): ResultState<String>


    suspend fun getVoiceListSupportAsync(languageCode: String): ResultState<List<Int>>
}