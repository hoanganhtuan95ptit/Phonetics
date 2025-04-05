package com.simple.phonetics.domain.usecase.phonetics

import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.state.ResultState
import com.simple.state.isCompleted
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Locale

class SyncPhoneticAsyncUseCase(
    private val appRepository: AppRepository,
    private val phoneticRepository: PhoneticRepository,
    private val languageRepository: LanguageRepository,
) {

    suspend fun execute(): Flow<ResultState<Map<String, State>>> = channelFlow {

        val configs = appRepository.getConfigsAsync().first()

        val inputLanguage = languageRepository.getLanguageInput() ?: return@channelFlow

        val lastTimeUpdateLocal = phoneticRepository.getLastTimeSyncPhonetic(language = inputLanguage)

        val lastTimeUpdateRemote = configs["language_${inputLanguage.id.lowercase()}_last_update"].runCatching {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(this ?: "")?.time ?: Long.MIN_VALUE
        }.getOrElse {
            Long.MIN_VALUE
        }


        val listState = linkedMapOf<String, State>()

        listState["START"] = State.Start
        trySend(ResultState.Running(listState))

        // nếu đã đồng bộ ipa thì không cần đông bộ nữa
        if (lastTimeUpdateLocal > lastTimeUpdateRemote) {

            listState["COMPLETED"] = State.Completed
            trySend(ResultState.Success(listState))
            awaitClose()
            return@channelFlow
        }

        // đồng bộ IPA
        val ipaState = phoneticRepository.syncPhonetic(language = inputLanguage, limit = 1000).filter {

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

        listState["COMPLETED"] = State.Completed
        trySend(ResultState.Success(listState))

        awaitClose()
    }

    class Param

    sealed class State(val value: Int = 0) {

        data object Start : State()

        data object Completed : State(Int.MAX_VALUE)

        data class SyncPhonetics(val code: String, val name: String, val percent: Float) : State(1)
    }
}
