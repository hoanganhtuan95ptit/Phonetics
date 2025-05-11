package com.simple.phonetics.domain.repositories

import com.simple.dao.entities.Ipa
import kotlinx.coroutines.flow.Flow

interface IpaRepository {

    suspend fun syncIpa(languageCode: String): List<Ipa>

    suspend fun getIpaAsync(languageCode: String): Flow<List<Ipa>>

    suspend fun insertOrUpdate(languageCode: String, list: List<Ipa>)

    suspend fun getCount(languageCode: String): Int

    suspend fun getCountAsync(languageCode: String): Flow<Int>

}