package com.simple.phonetics.data.tasks

import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.tasks.SyncTask

class LanguageSyncTask(
    private val languageRepository: LanguageRepository
) : SyncTask {

    override fun priority(): Int {
        return Int.MAX_VALUE - 3
    }

    override suspend fun executeTask(param: SyncTask.Param) {

        val languageCode = param.outputLanguage.id

        languageRepository.getLanguageSupport(languageCode = languageCode)
    }
}