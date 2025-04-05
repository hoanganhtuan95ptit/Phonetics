package com.simple.phonetics.data.tasks

import com.simple.analytics.logAnalytics
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.tasks.SyncTask
import kotlinx.coroutines.flow.first

class TranslateSyncTask(
    private val appRepository: AppRepository,
    private val languageRepository: LanguageRepository
) : SyncTask {

    private var languageCodeOld: String? = null

    override fun priority(): Int {
        return Int.MAX_VALUE
    }

    override suspend fun executeTask(param: SyncTask.Param) {

        // đông bộ dự liệu translate cú sang bảng mới
        copyTranslate()

        val languageCode = languageRepository.getLanguageOutputAsync().first().id

        if (languageCodeOld == languageCode) return

        // call api để lấy bản dịch
        val map = appRepository.syncTranslate(languageCode = languageCode)
        appRepository.updateTranslate(languageCode = languageCode, map = map)

        languageCodeOld = languageCode
    }

    private suspend fun copyTranslate() = runCatching {

        if (appRepository.getCountTranslate() > 0) return@runCatching

        logAnalytics("copy_translate")

        appRepository.getAllTranslateOld().groupBy { it.langCode }.mapValues { entry ->

            entry.value.map { it.key to it.value }.toMap()
        }.map {

            appRepository.updateTranslate(languageCode = it.key, map = it.value)
        }
    }.getOrElse {

        logCrashlytics("copy_translate", it)
    }
}