package com.simple.phonetics.data.repositories

import com.phonetics.word.dao.WordProvider
import com.simple.phonetics.data.api.ApiProvider
import com.simple.phonetics.data.dao.word.WordOldProvider
import com.simple.phonetics.domain.repositories.WordRepository
import com.simple.phonetics.entities.Word
import kotlinx.coroutines.flow.Flow

class WordRepositoryImpl(
    private val apiProvider: ApiProvider,
    private val wordProvider: WordProvider,
    private val wordOldProvider: WordOldProvider
) : WordRepository {

    private val api by lazy {
        apiProvider.api
    }

    private val wordDaoNew by lazy {
        wordProvider.wordDao
    }

    private val wordDaoOld by lazy {
        wordOldProvider.wordDao
    }

    override suspend fun syncPopular(languageCode: String): List<String> {
        return api.syncPopular(languageCode = languageCode)
    }

    override suspend fun insertOrUpdate(resource: String, languageCode: String, list: List<String>) {
        wordDaoNew.insertOrUpdate(resource = resource, languageCode = languageCode, list = list)
    }

    override suspend fun getAllOld(): List<Word> {
        return wordDaoOld.getAll()
    }

    override suspend fun getCountOLd(): Int {
        return wordDaoOld.getCount()
    }

    override suspend fun getCount(): Int {
        return wordDaoNew.getCount()
    }

    override suspend fun getCount(resource: String, languageCode: String): Int {
        return wordDaoNew.getCount(resource = resource, languageCode = languageCode)
    }

    override suspend fun getCountAsync(resource: String, languageCode: String): Flow<Int> {
        return wordDaoNew.getCountAsync(resource = resource, languageCode = languageCode)
    }

    override suspend fun getRandom(resource: String, languageCode: String, textMin: Int, textLimit: Int, limit: Int): List<String> {
        return wordDaoNew.getRandom(resource = resource, languageCode = languageCode, textMin = textMin, textLimit = textLimit, limit = limit)
    }
}