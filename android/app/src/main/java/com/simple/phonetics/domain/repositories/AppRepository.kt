package com.simple.phonetics.domain.repositories

import com.simple.phonetics.entities.KeyTranslate
import com.simple.state.ResultState
import com.simple.translate.entities.TranslateResponse
import kotlinx.coroutines.flow.Flow

interface AppRepository {

    suspend fun getKeyTranslate(langCode: String): List<KeyTranslate>

    suspend fun getKeyTranslateAsync(langCode: String): Flow<List<KeyTranslate>>

    suspend fun updateKeyTranslate(list: List<KeyTranslate>)

    suspend fun getKeyTranslateDefault(): Map<String, String>


    suspend fun translate(languageCodeInput: String, languageCodeOutput: String, vararg text: String): ResultState<List<TranslateResponse>>
}