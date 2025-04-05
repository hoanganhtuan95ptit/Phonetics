package com.simple.phonetics.data.tasks

import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.tasks.SyncTask

class ConfigSyncTask(
    private val appRepository: AppRepository,
) : SyncTask {

    private var sync: Boolean = false

    override fun priority(): Int {
        return Int.MIN_VALUE
    }

    override suspend fun executeTask(param: SyncTask.Param) {

        if (sync) return

        val events: Map<String, String> = appRepository.syncConfigs()
        appRepository.updateConfigs(events)

        sync = true
    }
}