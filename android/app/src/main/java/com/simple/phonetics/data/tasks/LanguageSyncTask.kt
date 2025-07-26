package com.simple.phonetics.data.tasks

import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.tasks.SyncTask
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

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

        languageRepository.runCatching {

            getLanguageSupport(languageCode = languageCode)
        }.getOrElse {

            if (it !is HttpException || it.code() != 404) logCrashlytics("language_sync_$languageCode", it)
            return
        }

        languageCodeOld = languageCode
    }
}