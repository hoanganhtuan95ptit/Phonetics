package com.simple.phonetics.data.repositories

import com.simple.phonetics.DEFAULT_TRANSLATE
import com.simple.phonetics.data.api.Api
import com.simple.phonetics.data.dao.KeyTranslateDao
import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.entities.KeyTranslate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppRepositoryImpl(
    private val api: Api,
    private val keyTranslateDao: KeyTranslateDao,
) : AppRepository {

    override suspend fun getKeyTranslate(langCode: String): List<KeyTranslate> {

        return api.syncTranslate(langCode).map {
            KeyTranslate(
                key = it.key,
                value = it.value,
                langCode = langCode
            )
        }
    }

    override suspend fun getKeyTranslateAsync(langCode: String): Flow<List<KeyTranslate>> {
        return keyTranslateDao.getAllAsync(langCode).map {
            it.map {
                KeyTranslate(it.key, it.value, it.langCode)
            }
        }
    }

    override suspend fun updateKeyTranslate(list: List<KeyTranslate>) {
        keyTranslateDao.insert(*list.toTypedArray())
    }

    override suspend fun getKeyTranslateDefault(): Map<String, String> {
        return DEFAULT_TRANSLATE
    }
}