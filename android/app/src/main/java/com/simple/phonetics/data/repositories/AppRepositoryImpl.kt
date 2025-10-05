package com.simple.phonetics.data.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.simple.coreapp.utils.extentions.postValue
import com.simple.phonetics.DEFAULT_TRANSLATE
import com.simple.phonetics.data.api.ApiProvider
import com.simple.phonetics.data.cache.AppCache
import com.simple.phonetics.data.dao.PhoneticRoomDatabaseProvider
import com.simple.phonetics.data.dao.translate.TranslateProvider
import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.repositories.DetectRepository
import com.simple.phonetics.domain.repositories.TranslateRepository
import com.simple.phonetics.entities.Event
import com.simple.phonetics.entities.KeyTranslate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AppRepositoryImpl(
    private val appCache: AppCache,
    private val apiProvider: ApiProvider,
    private val translateProvider: TranslateProvider,
    private val phoneticRoomDatabaseProvider: PhoneticRoomDatabaseProvider
) : AppRepository,
    DetectRepository by DetectRepositoryImpl(),
    TranslateRepository by TranslateRepositoryImpl() {

    private val api by lazy {
        apiProvider.api
    }

    private val translateDao by lazy {
        translateProvider.translateDao
    }

    private val keyTranslateDao by lazy {
        phoneticRoomDatabaseProvider.keyTranslateDao
    }


    private val events: LiveData<List<Event>> = MutableLiveData()

    private val configs: LiveData<Map<String, String>> = MutableLiveData()


    override suspend fun getCountTranslateOld(): Int {
        return keyTranslateDao.count()
    }

    override suspend fun getAllTranslateOld(): List<KeyTranslate> {
        return keyTranslateDao.getAll()
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

    override suspend fun syncConfigs(): Map<String, String> {
        return api.syncConfig()
    }

    override suspend fun getConfigsAsync(): Flow<Map<String, String>> {
        return configs.asFlow()
    }

    override suspend fun updateConfigs(map: Map<String, String>) {
        configs.postValue(map)
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