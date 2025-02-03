package com.simple.phonetics.domain.repositories

import com.simple.phonetics.entities.Language
import kotlinx.coroutines.flow.Flow

interface LanguageRepository {

    fun getLanguageInput(): Language?

    fun getLanguageInputAsync(): Flow<Language>

    fun updateLanguageInput(language: Language)


    fun getLanguageOutput(): Language

    fun getLanguageOutputAsync(): Flow<Language>


    suspend fun syncLanguageSupport(languageCode: String): List<Language>

    suspend fun getLanguageSupportedOrDefaultAsync(): Flow<List<Language>>
}