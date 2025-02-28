package com.simple.phonetics.domain.repositories

import com.simple.phonetics.entities.Ipa
import kotlinx.coroutines.flow.Flow

interface IpaRepository {

    suspend fun syncIpa(languageCode: String): List<Ipa>

    suspend fun getIpaAsync(languageCode: String): Flow<List<Ipa>>

    suspend fun insertOrUpdate(languageCode: String, list: List<Ipa>)
}