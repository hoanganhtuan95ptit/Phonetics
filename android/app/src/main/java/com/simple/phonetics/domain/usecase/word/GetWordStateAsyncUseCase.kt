package com.simple.phonetics.domain.usecase.word

import com.simple.coreapp.utils.ext.launchCollect
import com.simple.phonetics.domain.repositories.HistoryRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.domain.repositories.WordRepository
import com.simple.phonetics.entities.Word
import com.simple.phonetics.utils.exts.getWordDelimiters
import com.simple.phonetics.utils.exts.getWords
import com.simple.state.ResultState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest

class GetWordStateAsyncUseCase(
    private val wordRepository: WordRepository,
    private val historyRepository: HistoryRepository,
    private val phoneticRepository: PhoneticRepository,
    private val languageRepository: LanguageRepository
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun execute(): Flow<ResultState<Int>> = channelFlow {

        languageRepository.getLanguageInputAsync().launchCollect(this) {

            val languageCode = it.id

            syncPopular(languageCode = languageCode)
            syncHistory(languageCode = languageCode)
        }

        languageRepository.getLanguageInputAsync().flatMapLatest {

            val languageCode = it.id

            wordRepository.getCountAsync(resource = Word.Resource.Popular.value, languageCode = languageCode)
        }.launchCollect(this) {

            trySend(ResultState.Success(it))
        }

        awaitClose {
        }
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

    data class Param(val sync: Boolean = true)
}