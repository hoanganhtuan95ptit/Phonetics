package com.simple.phonetics.data.tasks

import com.simple.phonetics.domain.repositories.IpaRepository
import com.simple.phonetics.domain.tasks.SyncTask

class IpaSyncTask(
    private val ipaRepository: IpaRepository
) : SyncTask {

    override fun priority(): Int {
        return Int.MAX_VALUE - 4
    }

    override suspend fun executeTask(param: SyncTask.Param) {

        val languageCode = param.inputLanguage.id

        val ipaList = ipaRepository.syncIpa(languageCode = languageCode)

        ipaRepository.insertOrUpdate(languageCode = languageCode, list = ipaList)
    }
}