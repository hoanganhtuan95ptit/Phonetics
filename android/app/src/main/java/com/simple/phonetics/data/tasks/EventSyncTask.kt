package com.simple.phonetics.data.tasks

import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.tasks.SyncTask
import com.simple.phonetics.entities.Event

class EventSyncTask(
    private val appRepository: AppRepository,
) : SyncTask {

    override fun priority(): Int {
        return Int.MAX_VALUE - 1
    }

    override suspend fun executeTask(param: SyncTask.Param) {

        val languageCode = param.outputLanguage.id

        val events: List<Event> = appRepository.syncEvents(languageCode = languageCode)

        appRepository.updateEvents(events)
    }
}