package com.simple.phonetics.domain.repositories

import com.simple.phonetic.entities.Phonetic
import com.simple.phonetics.entities.Language
import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow

interface PhoneticRepository {


    suspend fun copy()

    suspend fun copyStateAsync(): Flow<ResultState<Float>>


    suspend fun syncPhonetic(language: Language, limit: Int = 10 * 1000): Flow<ResultState<Pair<Language.IpaSource, Float>>>

    suspend fun getLastTimeSyncPhonetic(language: Language): Long


    suspend fun getPhonetic(textList: List<String>): List<Phonetic>

    suspend fun getPhonetic(phoneticCode: String, textList: List<String>): List<Phonetic>

    suspend fun getPhonetic(ipaQuery: String, phoneticCode: String, textList: List<String>): List<Phonetic>


    suspend fun suggest(text: String): List<Phonetic>
}