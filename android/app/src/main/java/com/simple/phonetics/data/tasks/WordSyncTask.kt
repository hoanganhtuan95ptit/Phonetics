package com.simple.phonetics.data.tasks

import com.simple.analytics.logAnalytics
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.domain.repositories.HistoryRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.domain.repositories.WordRepository
import com.simple.phonetics.domain.tasks.SyncTask
import com.simple.phonetics.entities.Word
import com.simple.phonetics.utils.exts.getWordDelimiters
import com.simple.phonetics.utils.exts.getWords
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

class WordSyncTask(
    private val wordRepository: WordRepository,
    private val historyRepository: HistoryRepository,
    private val phoneticRepository: PhoneticRepository,
    private val languageRepository: LanguageRepository
) : SyncTask {

    private var languageCodeOld: String? = null

    override fun priority(): Int {
        return Int.MAX_VALUE - 2
    }

    override suspend fun executeTask(param: SyncTask.Param) {

        val languageCode = languageRepository.getLanguageInputAsync().filterNotNull().first().id

        if (languageCodeOld == languageCode) return

        copy()

        syncPopular(languageCode = languageCode)
        syncHistory(languageCode = languageCode)

        languageCodeOld = languageCode
    }

    /**
     * copy word
     * todo nếu không còn thấy event word_copy thì bỏ qua
     */
    @Deprecated("")
    private suspend fun copy() = runCatching {

        if (wordRepository.getCount() > 0) return@runCatching
        if (wordRepository.getCountOLd() <= 0) return@runCatching

        logAnalytics("word_copy")

        wordRepository.getAllOld().groupBy {

            it.resource to it.languageCode
        }.map {

            wordRepository.insertOrUpdate(resource = it.key.first.value, it.key.second, it.value.map { it.text })
        }
    }.getOrElse {

        logCrashlytics("word_copy", it)
    }

    /**
     * đồng bộ những từ phổ biến
     */
    private suspend fun syncPopular(languageCode: String) = runCatching {

        logAnalytics("word_sync_popular_start")

        // đồng bộ popular
        wordRepository.syncWord(languageCode = languageCode).forEach {
            wordRepository.insertOrUpdate(resource = it.name, languageCode = languageCode, list = it.words)
        }

        logAnalytics("word_sync_popular_success")
    }.getOrElse {

        if (it !is HttpException || it.code() != 404) logCrashlytics("word_sync_popular_$languageCode", it)
    }

    /**
     * dồng bộ lịch sử
     * todo nếu không còn thấy event sync_history thì bỏ qua
     */
    @Deprecated("")
    private suspend fun syncHistory(languageCode: String) = runCatching {

        val resource = Word.Resource.History.value

        // nếu trong data đã có thì không đồng bộ nữa
        if (wordRepository.getCount(resource = resource, languageCode = languageCode) > 0) return@runCatching

        val historyList = historyRepository.get(limit = 100)
        if (historyList.isEmpty()) return@runCatching

        logAnalytics("sync_history")

        // đồng bộ history
        val wordDelimiters = getWordDelimiters(languageCode = languageCode)

        // lấy ra từ đã tra trước đây, sắp xếp theo thứ tự tra nhiều nhất, và tối đã là 150
        val wordList = historyList.flatMap {

            it.getWords(wordDelimiters)
        }.groupBy {

            it.lowercase()
        }.mapValues {

            it.value.size
        }.toList().sortedByDescending {

            it.second
        }.toMap().keys.toMutableList().runCatching {

            subList(0, kotlin.math.min(size, 150))
        }.getOrNull().orEmpty()

        // chỉ lấy những từ có IPA
        val wordAvailableList = phoneticRepository.getPhonetic(wordList).filter {
            it.ipaValue.isNotEmpty()
        }.map {
            it.text
        }

        wordRepository.insertOrUpdate(resource = resource, languageCode = languageCode, list = wordAvailableList)
    }.getOrElse {

        logCrashlytics("word_sync_history", it)
    }
}