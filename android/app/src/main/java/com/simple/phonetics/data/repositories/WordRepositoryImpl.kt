package com.simple.phonetics.data.repositories

import com.phonetics.word.dao.WordDaoV2
import com.simple.phonetics.data.api.ApiProvider
import com.simple.phonetics.data.dao.word.WordDao
import com.simple.phonetics.domain.repositories.WordRepository
import com.simple.phonetics.entities.Word
import kotlinx.coroutines.flow.Flow

class WordRepositoryImpl(
    private val apiProvider: ApiProvider,
    private val wordDao: WordDao,
    private val wordDaoV2: WordDaoV2
) : WordRepository {

    override suspend fun syncPopular(languageCode: String): List<String> {
        return apiProvider.api.syncPopular(languageCode = languageCode)
    }

    override suspend fun insertOrUpdate(resource: String, languageCode: String, list: List<String>) {
        wordDaoV2.insertOrUpdate(resource = resource, languageCode = languageCode, list = list)
    }

    override suspend fun getAllOld(): List<Word> {
        return wordDao.getAll()
    }

    override suspend fun getCountOLd(): Int {
        return wordDao.getCount()
    }

    override suspend fun getCount(): Int {
        return wordDaoV2.getCount()
    }

    override suspend fun getCount(resource: String, languageCode: String): Int {
        return wordDaoV2.getCount(resource = resource, languageCode = languageCode)
    }

    override suspend fun getCountAsync(resource: String, languageCode: String): Flow<Int> {
        return wordDaoV2.getCountAsync(resource = resource, languageCode = languageCode)
    }

    override suspend fun getRandom(resource: String, languageCode: String, textMin: Int, textLimit: Int, limit: Int): List<String> {
        return wordDaoV2.getRandom(resource = resource, languageCode = languageCode, textMin = textMin, textLimit = textLimit, limit = limit)
    }
}