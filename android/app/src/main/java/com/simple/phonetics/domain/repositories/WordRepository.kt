package com.simple.phonetics.domain.repositories

import com.simple.phonetics.entities.Word
import com.simple.phonetics.entities.WordTopic
import com.simple.word.entities.WordResourceCount
import kotlinx.coroutines.flow.Flow

interface WordRepository {

    suspend fun syncWord(languageCode: String): List<WordTopic>

    suspend fun syncPopular(languageCode: String): List<String>

    suspend fun insertOrUpdate(resource: String, languageCode: String, list: List<String>)


    @Deprecated("")
    suspend fun getAllOld(): List<Word>

    @Deprecated("")
    suspend fun getCountOLd(): Int


    suspend fun getCount(): Int


    suspend fun getCount(resource: String, languageCode: String): Int

    suspend fun getCountAsync(resource: String, languageCode: String): Flow<Int>


    suspend fun getListWordResourceCountAsync(languageCode: String): Flow<List<WordResourceCount>>


    suspend fun getRandom(resource: String, languageCode: String, textMin: Int, textLimit: Int, limit: Int): List<String>
}