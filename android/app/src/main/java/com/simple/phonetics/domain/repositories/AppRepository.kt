package com.simple.phonetics.domain.repositories

import com.simple.phonetics.entities.Event
import com.simple.phonetics.entities.KeyTranslate
import kotlinx.coroutines.flow.Flow

interface AppRepository :
    DetectRepository,
    TranslateRepository {

    @Deprecated("remove")
    suspend fun getCountTranslateOld(): Int

    @Deprecated("remove")
    suspend fun getAllTranslateOld(): List<KeyTranslate>


    suspend fun syncTranslate(languageCode: String): Map<String, String>

    suspend fun updateTranslate(languageCode: String, map: Map<String, String>)

    suspend fun getTranslateAsync(languageCode: String): Flow<Map<String, String>>

    suspend fun getCountTranslate(): Int

    suspend fun getKeyTranslateDefault(): Map<String, String>


    suspend fun syncConfigs(): Map<String, String>

    suspend fun getConfigsAsync(): Flow<Map<String, String>>

    suspend fun updateConfigs(map: Map<String, String>)


    fun getEventIdShow(): String?

    fun updateEventIdShow(id: String)

    suspend fun syncEvents(languageCode: String): List<Event>

    suspend fun getEventsAsync(): Flow<List<Event>>

    suspend fun updateEvents(list: List<Event>)


    suspend fun getTranslateSelected(): String

    suspend fun getTranslateSelectedAsync(): Flow<String>

    suspend fun updateTranslateSelected(translateSelected: String)
}