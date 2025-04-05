package com.simple.phonetics.domain.repositories

import com.simple.phonetics.entities.Event
import com.simple.phonetics.entities.KeyTranslate
import com.simple.state.ResultState
import com.simple.translate.entities.TranslateResponse
import kotlinx.coroutines.flow.Flow

interface AppRepository {

    @Deprecated("remove")
    suspend fun getAllTranslateOld(): List<KeyTranslate>

    @Deprecated("remove")
    suspend fun getKeyTranslate(langCode: String): List<KeyTranslate>

    @Deprecated("remove")
    suspend fun getKeyTranslateAsync(langCode: String): Flow<List<KeyTranslate>>


    suspend fun syncTranslate(languageCode: String): Map<String, String>

    suspend fun updateTranslate(languageCode: String, map: Map<String, String>)

    suspend fun getTranslateAsync(languageCode: String): Flow<Map<String, String>>

    suspend fun getCountTranslate(): Int


    suspend fun getKeyTranslateDefault(): Map<String, String>


    suspend fun translate(languageCodeInput: String, languageCodeOutput: String, vararg text: String): ResultState<List<TranslateResponse>>


    fun getEventIdShow(): String?

    fun updateEventIdShow(id: String)

    suspend fun syncEvents(languageCode: String): List<Event>

    suspend fun getEventsAsync(): Flow<List<Event>>

    suspend fun updateEvents(list: List<Event>)

}