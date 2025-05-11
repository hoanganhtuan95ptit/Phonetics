package com.simple.phonetics.domain.repositories

import com.simple.phonetics.entities.Language
import kotlinx.coroutines.flow.Flow

interface LanguageRepository {

    suspend fun getPhoneticCodeSelected(): String

    suspend fun getPhoneticCodeSelectedAsync(): Flow<String>

    suspend fun updatePhoneticCodeSelected(code: String)


    fun getLanguageInput(): Language?

    fun getLanguageInputAsync(): Flow<Language>

    fun updateLanguageInput(language: Language)


    fun getLanguageOutput(): Language

    fun getLanguageOutputAsync(): Flow<Language>


    suspend fun getLanguageSupport(languageCode: String): List<Language>

    suspend fun getLanguageSupportedOrDefaultAsync(): Flow<List<Language>>
}