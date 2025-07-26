package com.simple.phonetics.data.tasks

import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.repositories.HistoryRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.tasks.SyncTask
import com.simple.phonetics.entities.Event
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

class EventSyncTask(
    private val appRepository: AppRepository,
    private val historyRepository: HistoryRepository,
    private val languageRepository: LanguageRepository
) : SyncTask {

    private var languageCodeOld: String? = null

    override fun priority(): Int {
        return Int.MAX_VALUE - 1
    }

    override suspend fun executeTask(param: SyncTask.Param) {

        // nếu không có lịch sử thì không cần đồng bộ event nữa
        if (historyRepository.get(limit = 1).isEmpty()) {
            return
        }

        val languageCode = languageRepository.getLanguageOutputAsync().first().id

        if (languageCodeOld == languageCode) return

        runCatching {

            val events: List<Event> = appRepository.syncEvents(languageCode = languageCode)
            appRepository.updateEvents(events)
        }.getOrElse {

            appRepository.updateEvents(emptyList())
            if (it !is HttpException || it.code() != 404) logCrashlytics("event_sync_$languageCode", it)
            return
        }

        languageCodeOld = languageCode
    }
}