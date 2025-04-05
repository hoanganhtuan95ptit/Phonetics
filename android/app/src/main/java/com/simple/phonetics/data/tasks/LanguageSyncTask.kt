package com.simple.phonetics.data.tasks

import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.tasks.SyncTask
import kotlinx.coroutines.flow.first

class LanguageSyncTask(
    private val languageRepository: LanguageRepository
) : SyncTask {

    private var languageCodeOld: String? = null

    override fun priority(): Int {
        return Int.MAX_VALUE - 3
    }

    override suspend fun executeTask(param: SyncTask.Param) {

        val languageCode = languageRepository.getLanguageOutputAsync().first().id

        if (languageCodeOld == languageCode) return

        languageRepository.getLanguageSupport(languageCode = languageCode)

        languageCodeOld = languageCode
    }
}