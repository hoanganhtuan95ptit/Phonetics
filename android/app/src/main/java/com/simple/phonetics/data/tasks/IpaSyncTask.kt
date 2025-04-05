package com.simple.phonetics.data.tasks

import com.simple.phonetics.domain.repositories.IpaRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.tasks.SyncTask
import kotlinx.coroutines.flow.first

class IpaSyncTask(
    private val ipaRepository: IpaRepository,
    private val languageRepository: LanguageRepository
) : SyncTask {

    private var languageCodeOld: String? = null

    override fun priority(): Int {
        return Int.MAX_VALUE - 4
    }

    override suspend fun executeTask(param: SyncTask.Param) {

        val languageCode = languageRepository.getLanguageInputAsync().first().id

        if (languageCodeOld == languageCode) return

        val ipaList = ipaRepository.syncIpa(languageCode = languageCode)
        ipaRepository.insertOrUpdate(languageCode = languageCode, list = ipaList)

        languageCodeOld = languageCode
    }
}