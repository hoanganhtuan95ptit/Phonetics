package com.simple.phonetics.domain.repositories

import kotlinx.coroutines.flow.Flow

interface WordRepository {

    suspend fun syncPopular(languageCode: String): List<String>

    suspend fun insertOrUpdate(resource: String, languageCode: String, list: List<String>)

    suspend fun getCount(resource: String, languageCode: String): Int

    suspend fun getCountAsync(resource: String, languageCode: String): Flow<Int>

    suspend fun getRandom(resource: String, languageCode: String, textLimit: Int, limit: Int): List<String>
}