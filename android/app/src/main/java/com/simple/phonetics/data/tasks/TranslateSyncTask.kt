package com.simple.phonetics.data.tasks

import com.simple.analytics.logAnalytics
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.tasks.SyncTask

class TranslateSyncTask(
    private val appRepository: AppRepository,
) : SyncTask {

    override fun priority(): Int {
        return Int.MAX_VALUE
    }

    override suspend fun executeTask(param: SyncTask.Param) {

        // đông bộ dự liệu translate cú sang bảng mới
        copyTranslate()

        // call api để lấy bản dịch
        val languageCode = param.outputLanguage.id

        val map = appRepository.syncTranslate(languageCode = languageCode)

        appRepository.updateTranslate(languageCode = languageCode, map = map)
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