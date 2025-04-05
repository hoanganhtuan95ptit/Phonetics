package com.simple.phonetics.data.tasks

import com.simple.phonetics.domain.repositories.HistoryRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.domain.repositories.WordRepository
import com.simple.phonetics.domain.tasks.SyncTask
import com.simple.phonetics.entities.Word
import com.simple.phonetics.utils.exts.getWordDelimiters
import com.simple.phonetics.utils.exts.getWords
import kotlinx.coroutines.flow.first

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

        val languageCode = languageRepository.getLanguageInputAsync().first().id

        if (languageCodeOld == languageCode) return

        syncPopular(languageCode = languageCode)
        syncHistory(languageCode = languageCode)

        languageCodeOld = languageCode
    }

    /**
     * đồng bộ những từ phổ biến
     */
    private suspend fun syncPopular(languageCode: String) = runCatching {

        val resource = Word.Resource.Popular.value

        // nếu trong data đã có thì không đồng bộ nữa
        if (wordRepository.getCount(resource = resource, languageCode = languageCode) > 0) return@runCatching

        // đồng bộ popular
        val list = wordRepository.syncPopular(languageCode = languageCode)

        wordRepository.insertOrUpdate(resource = resource, languageCode = languageCode, list = list)
    }

    /**
     * dồng bộ lịch sử
     */
    private suspend fun syncHistory(languageCode: String) = runCatching {

        val resource = Word.Resource.History.value

        // nếu trong data đã có thì không đồng bộ nữa
        if (wordRepository.getCount(resource = resource, languageCode = languageCode) > 0) return@runCatching

        val historyList = historyRepository.get(limit = 100)
        if (historyList.isEmpty()) return@runCatching

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
        val wordAvailableList = phoneticRepository.getPhonetics(wordList).filter {
            it.ipa.isNotEmpty()
        }.map {
            it.text
        }

        wordRepository.insertOrUpdate(resource = resource, languageCode = languageCode, list = wordAvailableList)
    }
}