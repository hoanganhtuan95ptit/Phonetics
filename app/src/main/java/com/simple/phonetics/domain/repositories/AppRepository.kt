package com.simple.phonetics.domain.repositories

import com.simple.phonetics.entities.KeyTranslate
import kotlinx.coroutines.flow.Flow

interface AppRepository {


    suspend fun getLanguageCode(): String

    suspend fun setLanguageCode(langCode: String)

    suspend fun getLanguageCodeAsync(): Flow<String>


    suspend fun getKeyTranslate(langCode: String): List<KeyTranslate>

    suspend fun getKeyTranslateDefault(): Map<String, String>

    suspend fun setKeyTranslate(list: List<KeyTranslate>)

    suspend fun getKeyTranslateAsync(langCode: String): Flow<List<KeyTranslate>>
}