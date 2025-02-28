package com.simple.phonetics.domain.usecase.ipa

import com.simple.coreapp.utils.ext.launchCollect
import com.simple.phonetics.domain.repositories.IpaRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.entities.Ipa
import com.simple.state.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest

class GetIpaStateAsyncUseCase(
    private val ipaRepository: IpaRepository,
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(param: Param = Param()): Flow<ResultState<List<Ipa>>> = channelFlow {

        if (param.sync) languageRepository.getLanguageInputAsync().launchCollect(this) {

            runCatching {

                val languageCode = it.id

                val ipaList = ipaRepository.syncIpa(languageCode = languageCode)

                ipaRepository.insertOrUpdate(languageCode = languageCode, list = ipaList)
            }
        }

        languageRepository.getLanguageInputAsync().flatMapLatest {

            val languageCode = it.id

            ipaRepository.getIpaAsync(languageCode = languageCode)
        }.launchCollect(this) {

            trySend(ResultState.Success(it))
        }

        awaitClose {
        }
    }

    data class Param(val sync: Boolean = true)
}