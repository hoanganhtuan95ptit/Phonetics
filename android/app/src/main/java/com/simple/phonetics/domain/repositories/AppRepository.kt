package com.simple.phonetics.domain.repositories

import com.simple.state.ResultState
import com.simple.translate.entities.TranslateResponse
import kotlinx.coroutines.flow.Flow

interface AppRepository {

    suspend fun syncTranslate(languageCode: String): Map<String, String>

    suspend fun updateTranslate(languageCode: String, map: Map<String, String>)

    suspend fun getTranslateAsync(languageCode: String): Flow<Map<String, String>>


    suspend fun getKeyTranslateDefault(): Map<String, String>


    suspend fun translate(languageCodeInput: String, languageCodeOutput: String, vararg text: String): ResultState<List<TranslateResponse>>
}