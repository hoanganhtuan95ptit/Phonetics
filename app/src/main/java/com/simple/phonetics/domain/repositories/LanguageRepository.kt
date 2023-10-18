package com.simple.phonetics.domain.repositories

import com.simple.phonetics.domain.entities.Language
import kotlinx.coroutines.flow.Flow

interface LanguageRepository {

    fun getLanguageInput(): Language

    fun getLanguageInputAsync(): Flow<Language>


    fun getLanguageOutput(): Language

    fun getLanguageOutputAsync(): Flow<Language>
}