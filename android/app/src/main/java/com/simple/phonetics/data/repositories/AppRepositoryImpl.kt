package com.simple.phonetics.data.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.simple.coreapp.utils.extentions.postDifferentValue
import com.simple.coreapp.utils.extentions.postValue
import com.simple.phonetics.DEFAULT_TRANSLATE
import com.simple.phonetics.data.api.Api
import com.simple.phonetics.data.cache.AppCache
import com.simple.phonetics.data.dao.KeyTranslateDao
import com.simple.phonetics.data.dao.translate.TranslateDao
import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.entities.Event
import com.simple.phonetics.entities.KeyTranslate
import com.simple.state.ResultState
import com.simple.task.executeSyncByPriority
import com.simple.translate.data.tasks.TranslateTask
import com.simple.translate.entities.TranslateRequest
import com.simple.translate.entities.TranslateResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AppRepositoryImpl(
    private val api: Api,
    private val appCache: AppCache,
    private val translateDao: TranslateDao,
    private val keyTranslateDao: KeyTranslateDao,
    private val listTranslateTask: List<TranslateTask>
) : AppRepository {

    private val events: LiveData<List<Event>> = MutableLiveData()

    private val configs: LiveData<Map<String, String>> = MutableLiveData()

    override suspend fun getKeyTranslate(langCode: String): List<KeyTranslate> {

        return api.syncTranslate(langCode).map {
            KeyTranslate(
                key = it.key,
                value = it.value,
                langCode = langCode
            )
        }
    }

    override suspend fun getCountTranslateOld(): Int {
        return keyTranslateDao.count()
    }

    override suspend fun getAllTranslateOld(): List<KeyTranslate> {
        return keyTranslateDao.getAll()
    }

    override suspend fun getKeyTranslateAsync(langCode: String): Flow<List<KeyTranslate>> {

        return keyTranslateDao.getAllAsync(langCode).map {
            it.map {
                KeyTranslate(it.key, it.value, it.langCode)
            }
        }
    }


    override suspend fun syncTranslate(languageCode: String): Map<String, String> {
        return api.syncTranslate(languageCode = languageCode)
    }

    override suspend fun updateTranslate(languageCode: String, map: Map<String, String>) {
        translateDao.insertOrUpdate(languageCode = languageCode, map = map)
    }

    override suspend fun getTranslateAsync(languageCode: String): Flow<Map<String, String>> {
        return translateDao.getAllAsync(languageCode = languageCode)
    }

    override suspend fun getCountTranslate(): Int {
        return translateDao.getCount()
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


    override suspend fun syncConfigs(): Map<String, String> {
        return api.syncConfig()
    }

    override suspend fun getConfigsAsync(): Flow<Map<String, String>> {
        return configs.asFlow()
    }

    override suspend fun updateConfigs(map: Map<String, String>) {
        configs.postDifferentValue(map)
    }


    override fun getEventIdShow(): String? {
        return appCache.getData("EVENT_ID_SHOW", "")
    }

    override fun updateEventIdShow(id: String) {
        appCache.setData("EVENT_ID_SHOW", id)
    }

    override suspend fun syncEvents(languageCode: String): List<Event> {
        return api.syncEvent(languageCode = languageCode)
    }

    override suspend fun getEventsAsync(): Flow<List<Event>> {
        return events.asFlow()
    }

    override suspend fun updateEvents(list: List<Event>) {
        events.postValue(list)
    }


    override suspend fun getTranslateSelected(): String {

        return appCache.getData(TRANSLATE_STATUS, "0")
    }

    override suspend fun getTranslateSelectedAsync(): Flow<String> {

        return appCache.getDataAsync(TRANSLATE_STATUS).map {

            getTranslateSelected()
        }.distinctUntilChanged()
    }

    override suspend fun updateTranslateSelected(translateSelected: String) {

        appCache.setData(TRANSLATE_STATUS, translateSelected)
    }


    override fun <T> updateData(key: String, value: T) {

        appCache.setData(key, value)
    }

    override fun <T> getData(key: String, default: T): T {

        return appCache.getData(key, default)
    }

    override fun <T> getDataAsync(key: String, default: T): Flow<T> {

        return appCache.getDataAsync(key, default)
    }

    companion object {

        private const val TRANSLATE_STATUS = "translate_status"
    }
}