package com.simple.phonetics.data.repositories

import com.simple.phonetics.data.api.Api
import com.simple.phonetics.data.dao.word.WordDao
import com.simple.phonetics.domain.repositories.WordRepository
import kotlinx.coroutines.flow.Flow

class WordRepositoryImpl(
    private val api: Api,
    private val wordDao: WordDao
) : WordRepository {

    override suspend fun syncPopular(languageCode: String): List<String> {
        return api.syncPopular(languageCode = languageCode)
    }

    override suspend fun insertOrUpdate(resource: String, languageCode: String, list: List<String>) {
        wordDao.insertOrUpdate(resource = resource, languageCode = languageCode, list = list)
    }

    override suspend fun getCount(resource: String, languageCode: String): Int {
        return wordDao.getCount(resource = resource, languageCode = languageCode)
    }

    override suspend fun getCountAsync(resource: String, languageCode: String): Flow<Int> {
        return wordDao.getCountAsync(resource = resource, languageCode = languageCode)
    }

    override suspend fun getRandom(resource: String, languageCode: String, textMin: Int, textLimit: Int, limit: Int): List<String> {
        return wordDao.getRandom(resource = resource, languageCode = languageCode, textMin = textMin, textLimit = textLimit, limit = limit)
    }
}