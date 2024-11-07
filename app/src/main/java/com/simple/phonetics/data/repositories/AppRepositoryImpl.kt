package com.simple.phonetics.data.repositories

import com.simple.coreapp.ui.dialogs.options.Param.LANGUAGE_CODE
import com.simple.phonetics.DEFAULT_TRANSLATE
import com.simple.phonetics.data.cache.AppCache
import com.simple.phonetics.data.dao.KeyTranslateDao
import com.simple.phonetics.data.dao.KeyTranslateRoom
import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.entities.KeyTranslate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.Locale

class AppRepositoryImpl(
    private val appCache: AppCache,
    private val keyTranslateDao: KeyTranslateDao,
) : AppRepository {


    override suspend fun getLanguageCode(): String {
        return appCache.getDataAsync(LANGUAGE_CODE).firstOrNull()?.takeIf { it.isNotBlank() } ?: Locale.getDefault().language
    }

    override suspend fun setLanguageCode(langCode: String) {
        appCache.setData(LANGUAGE_CODE, langCode)
    }

    override suspend fun getLanguageCodeAsync(): Flow<String> {
        return appCache.getDataAsync(LANGUAGE_CODE).map {
            getLanguageCode()
        }
    }


    override suspend fun getKeyTranslate(langCode: String): List<KeyTranslate> {
        return emptyList()
    }

    override suspend fun getKeyTranslateDefault(): Map<String, String> {
        return DEFAULT_TRANSLATE
    }

    override suspend fun setKeyTranslate(list: List<KeyTranslate>) {
        keyTranslateDao.insert(*list.toTypedArray())
    }

    override suspend fun getKeyTranslateAsync(langCode: String): Flow<List<KeyTranslate>> {
        return keyTranslateDao.getAllAsync(langCode).map {
            it.map {
                KeyTranslate(it.key, it.value, it.langCode)
            }
        }
    }
}