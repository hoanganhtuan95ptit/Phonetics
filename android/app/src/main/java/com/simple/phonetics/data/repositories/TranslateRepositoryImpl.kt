package com.simple.phonetics.data.repositories

import com.simple.phonetics.domain.repositories.TranslateRepository
import com.simple.state.ResultState
import com.simple.state.isSuccess
import com.simple.state.runResultState
import com.tuanha.translate_2.TranslateTask
import com.tuanha.translate_2.entities.Translate
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TranslateRepositoryImpl : TranslateRepository {

    override suspend fun translateAwait(languageCodeInput: String, languageCodeOutput: String, vararg text: String): ResultState<List<Translate.Response>> {

        val input = text.map {

            Translate.Request(
                text = it,
                languageCode = languageCodeInput
            )
        }

        return TranslateTask.instant.first {

            it.isNotEmpty()
        }.first().runResultState {

            translate(input = input, outputLanguageCode = languageCodeOutput)
        }
    }

    override suspend fun checkSupportTranslateAsync(languageCodeInput: String, languageCodeOutput: String): Flow<ResultState<Boolean>> = channelFlow {

        trySend(ResultState.Start)

        val input = listOf(

            Translate.Request(
                text = "hello",
                languageCode = languageCodeInput
            )
        )

        TranslateTask.instant.map { list ->

            list.isNotEmpty() && list.any { it.translate(input = input, outputLanguageCode = languageCodeOutput).firstOrNull()?.state.isSuccess() }
        }.launchCollect(this) {

            val state = if (it) {
                ResultState.Success(true)
            } else {
                ResultState.Failed()
            }

            trySend(state)
        }

        awaitClose {
        }
    }
}