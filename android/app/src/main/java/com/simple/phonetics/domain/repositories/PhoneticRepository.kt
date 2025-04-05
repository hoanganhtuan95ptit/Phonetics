package com.simple.phonetics.domain.repositories

import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Phonetic
import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow

interface PhoneticRepository {

    suspend fun syncPhonetic(language: Language, limit: Int = 10 * 1000): Flow<ResultState<Pair<Language.IpaSource, Float>>>

    suspend fun getLastTimeSyncPhonetic(language: Language): Long

    suspend fun getPhonetics(phonetics: List<String>): List<Phonetic>

    suspend fun getPhonetics(textList: List<String>, phoneticCode: String): List<Phonetic>

    suspend fun insertOrUpdate(phonetics: List<Phonetic>)

    suspend fun getSourcePhonetic(it: Language.IpaSource): String
}