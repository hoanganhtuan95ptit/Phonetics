package com.simple.phonetics.domain.usecase.language

import com.simple.coreapp.utils.ext.launchCollect
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.entities.Language
import com.simple.state.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map

class GetLanguageSupportAsyncUseCase(
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(param: Param): Flow<ResultState<List<Language>>> = channelFlow {

        languageRepository.getLanguageSupportedOrDefaultAsync().launchCollect(this) {

            trySend(ResultState.Success(it))
        }

        if (param.sync) languageRepository.getLanguageOutputAsync().launchCollect(this) {

            runCatching {
                languageRepository.getLanguageSupport(it.id)
            }
        }

        awaitClose {

        }
    }

    data class Param(val sync: Boolean)
}