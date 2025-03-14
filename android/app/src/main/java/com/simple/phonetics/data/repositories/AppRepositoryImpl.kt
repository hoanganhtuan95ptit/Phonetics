package com.simple.phonetics.data.repositories

import com.simple.phonetics.DEFAULT_TRANSLATE
import com.simple.phonetics.data.api.Api
import com.simple.phonetics.data.dao.translate.TranslateDao
import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.state.ResultState
import com.simple.task.executeSyncByPriority
import com.simple.translate.data.tasks.TranslateTask
import com.simple.translate.entities.TranslateRequest
import com.simple.translate.entities.TranslateResponse
import kotlinx.coroutines.flow.Flow

class AppRepositoryImpl(
    private val api: Api,
    private val translateDao: TranslateDao,
    private val listTranslateTask: List<TranslateTask>
) : AppRepository {

    override suspend fun syncTranslate(languageCode: String): Map<String, String> {
        return api.syncTranslate(languageCode = languageCode)
    }

    override suspend fun updateTranslate(languageCode: String, map: Map<String, String>) {
        translateDao.insertOrUpdate(languageCode = languageCode, map = map)
    }

    override suspend fun getTranslateAsync(languageCode: String): Flow<Map<String, String>> {
        return translateDao.getAllAsync(languageCode = languageCode)
    }


    override suspend fun getKeyTranslateDefault(): Map<String, String> {
        return DEFAULT_TRANSLATE
    }


    override suspend fun translate(languageCodeInput: String, languageCodeOutput: String, vararg text: String): ResultState<List<TranslateResponse>> {

        val input = text.map {

            TranslateRequest(
                text = it,
                languageCode = languageCodeInput
            )
        }

        val translateState = listTranslateTask.executeSyncByPriority(TranslateTask.Param(input = input, outputCode = languageCodeOutput))

        return translateState
    }
}