package com.simple.phonetics.domain.usecase.word

import com.simple.coreapp.utils.ext.launchCollect
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.WordRepository
import com.simple.phonetics.entities.Word
import com.simple.state.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest

class GetPopularStateAsyncUseCase(
    private val wordRepository: WordRepository,
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(param: Param = Param()): Flow<ResultState<Int>> = channelFlow {

        if (param.sync) languageRepository.getLanguageInputAsync().launchCollect(this) {

            runCatching {

                val languageCode = it.id

                // nếu trong data đã có thì không đồng bộ nữa
                if (wordRepository.getCount(resource = Word.Resource.Popular.value, languageCode = languageCode) > 0) return@launchCollect

                // đồng bộ popular
                val list = wordRepository.syncPopular(languageCode = languageCode)
                wordRepository.insertOrUpdate(resource = Word.Resource.Popular.value, languageCode = languageCode, list = list)
            }
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

    data class Param(val sync: Boolean = true)
}