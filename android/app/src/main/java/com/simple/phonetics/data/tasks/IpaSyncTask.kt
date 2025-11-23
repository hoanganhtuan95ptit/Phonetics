package com.simple.phonetics.data.tasks

import com.simple.analytics.logAnalytics
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.domain.repositories.IpaRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.tasks.SyncTask
import com.simple.phonetics.entities.Language
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import retrofit2.HttpException

class IpaSyncTask(
    private val ipaRepository: IpaRepository,
    private val languageRepository: LanguageRepository
) : SyncTask {

    private var languageCodeOld: String? = null

    override fun priority(): Int {
        return Int.MAX_VALUE - 4
    }

    override suspend fun executeTask(param: SyncTask.Param) {

        val languageCode = languageRepository.getLanguageInputAsync().filterNotNull().first().id

        copy(languageCode = languageCode)

        if (languageCodeOld == languageCode) return


        val ipaList = runCatching {

            ipaRepository.syncIpa(languageCode = languageCode)
        }.getOrElse {

            if (it !is HttpException || it.code() != 404) logCrashlytics("ipa_sync_$languageCode", it)
            return
        }.map {

            if (languageCode == Language.EN && it.ipa == "/r/") it.copy(ipa = "/ɹ/") else it
        }

        if (languageCode == Language.EN) {

            ipaRepository.deleteByKey(languageCode = languageCode, ipa = "/r/")
        }

        ipaRepository.insertOrUpdate(languageCode = languageCode, list = ipaList)


        languageCodeOld = languageCode
    }

    /**
     * todo nếu không còn thấy event copy_ipa thì bỏ qua
     */
    @Deprecated("")
    private suspend fun copy(languageCode: String) = runCatching {

        if (ipaRepository.getCount(languageCode = languageCode) > 0) return@runCatching
        if (ipaRepository.countAlOld(languageCode = languageCode) <= 0) return@runCatching

        logAnalytics("copy_ipa")

        ipaRepository.getAllOldAsync(languageCode = languageCode).firstOrNull()?.let {

            ipaRepository.insertOrUpdate(languageCode = languageCode, it)
        }
    }.getOrElse {

        logCrashlytics("copy_ipa", it)
    }
}