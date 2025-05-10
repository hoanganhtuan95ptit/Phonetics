package com.simple.phonetics.domain.usecase.language

import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.entities.Language
import com.simple.state.ResultState
import com.simple.state.isCompleted
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UpdateLanguageInputUseCase(
    private val appRepository: AppRepository,
    private val phoneticRepository: PhoneticRepository,
    private val languageRepository: LanguageRepository,
) {

    suspend fun execute(param: Param): Flow<ResultState<Map<String, State>>> = channelFlow {

        val listState = linkedMapOf<String, State>()

        listState["START"] = State.Start
        trySend(ResultState.Running(listState))

        // đồng bộ IPA
        val ipaState = phoneticRepository.syncPhonetic(language = param.language).filter {

            if (it is ResultState.Running) {

                val source = it.data.first
                val percent = it.data.second

                listState[source.code] = State.SyncPhonetics(code = source.code, name = source.name, percent = percent)
                trySend(ResultState.Running(listState))
            }

            it.isCompleted()
        }.first()

        if (ipaState is ResultState.Failed) {

            trySend(ipaState)
            awaitClose()
            return@channelFlow
        }


        // Đồng bộ translate
        val translateState = syncTranslate(language = param.language).filter {

            if (it is ResultState.Running) {

                listState["SYNC_TRANSLATE"] = it.data
                trySend(ResultState.Running(listState))
            }

            it.isCompleted()
        }.first()

        if (translateState is ResultState.Failed) {

            trySend(translateState)
            awaitClose()
            return@channelFlow
        }

        // lưu lại ngôn ngữ tìm kiếm phiên âm
        languageRepository.updateLanguageInput(param.language)

        // hủy phonectic code hiện tại
        languageRepository.updatePhoneticCode("")


        listState["COMPLETED"] = State.Completed
        trySend(ResultState.Success(listState))


        awaitClose()
    }

    private fun syncTranslate(language: Language) = channelFlow {

        val job = launch {

            for (i in 0..90) {

                trySend(ResultState.Running(State.SyncTranslate(i / 100f)))

                delay(200)
            }

            delay(5 * 1000)

            trySend(ResultState.Success(State.SyncTranslate(100f)))
        }

        runCatching {

            val languageOutput = languageRepository.getLanguageOutput()

            appRepository.translate(language.id, languageOutput.id, "hello")
        }

        job.cancel()

        trySend(ResultState.Success(State.SyncTranslate(100f)))

        awaitClose {

        }
    }


    data class Param(
        val language: Language
    )

    sealed class State(val value: Int = 0) {

        data object Start : State()

        data object Completed : State(Int.MAX_VALUE)

        data class SyncTranslate(val percent: Float) : State(2)

        data class SyncPhonetics(val code: String, val name: String, val percent: Float) : State(1)
    }
}
