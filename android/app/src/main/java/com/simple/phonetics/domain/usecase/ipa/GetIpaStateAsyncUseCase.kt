package com.simple.phonetics.domain.usecase.ipa

import com.simple.ipa.entities.Ipa
import com.simple.phonetics.domain.repositories.IpaRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.state.ResultState
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

class GetIpaStateAsyncUseCase(
    private val ipaRepository: IpaRepository,
    private val languageRepository: LanguageRepository
) {

    fun execute(param: Param = Param()): Flow<ResultState<List<Ipa>>> = channelFlow {

        languageRepository.getLanguageInputAsync().filterNotNull().flatMapLatest {

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