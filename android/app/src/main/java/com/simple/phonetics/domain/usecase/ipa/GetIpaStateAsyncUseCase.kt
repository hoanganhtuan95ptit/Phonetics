package com.simple.phonetics.domain.usecase.ipa

import com.simple.coreapp.utils.ext.launchCollect
import com.simple.phonetics.domain.repositories.IpaRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.entities.Ipa
import com.simple.state.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

class GetIpaStateAsyncUseCase(
    private val ipaRepository: IpaRepository,
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(): Flow<ResultState<List<Ipa>>> = channelFlow {

        languageRepository.getLanguageInputAsync().launchCollect(this) {

            val ipaList = ipaRepository.syncIpa(languageCode = it.id)

            trySend(ResultState.Success(ipaList))
        }

        awaitClose {
        }
    }
}