package com.simple.phonetics.domain.repositories

import com.simple.phonetics.entities.Event
import com.simple.phonetics.entities.KeyTranslate
import com.simple.state.ResultState
import com.simple.translate.entities.TranslateResponse
import kotlinx.coroutines.flow.Flow

interface AppRepository {

    @Deprecated("remove")
    suspend fun getCountTranslateOld(): Int

    @Deprecated("remove")
    suspend fun getAllTranslateOld(): List<KeyTranslate>


    suspend fun syncTranslate(languageCode: String): Map<String, String>

    suspend fun updateTranslate(languageCode: String, map: Map<String, String>)

    suspend fun getTranslateAsync(languageCode: String): Flow<Map<String, String>>

    suspend fun getCountTranslate(): Int

    suspend fun getKeyTranslateDefault(): Map<String, String>


    suspend fun detect(languageCodeInput: String, languageCodeOutput: String, path: String): ResultState<String>

    suspend fun checkDetect(languageCodeInput: String, languageCodeOutput: String): Boolean

    suspend fun translate(languageCodeInput: String, languageCodeOutput: String, vararg text: String): ResultState<List<TranslateResponse>>

    suspend fun checkTranslate(languageCodeInput: String, languageCodeOutput: String): ResultState<Boolean>


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


    fun <T> updateData(key: String, value: T)

    fun <T> getData(key: String, default: T): T

    fun <T> getDataAsync(key: String, default: T): Flow<T>
}