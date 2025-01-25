package com.simple.phonetics.domain.usecase.language

import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.entities.Language
import com.simple.state.ResultState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

class UpdateLanguageInputUseCase(
    private val appRepository: AppRepository,
    private val phoneticRepository: PhoneticRepository,
    private val languageRepository: LanguageRepository,
) {

    suspend fun execute(param: Param): Flow<ResultState<Map<String, State>>> = channelFlow {

        val listState = linkedMapOf<String, State>()

        listState.put("start", State.START)
        trySend(ResultState.Running(listState))

        param.language.listIpa.forEach {

            listState.put(it.name, State.SYNC_PHONETICS(it.name, 0f))
            trySend(ResultState.Running(listState))

            var count = 0

            val data = kotlin.runCatching {

                phoneticRepository.getSourcePhonetic(it)
            }.getOrElse {

                trySend(ResultState.Failed(it))
                awaitClose()
                return@channelFlow
            }

            val limit = 10 * 1000
            val dataLength = data.length

            while (count < dataLength) {

                val start = count
                val end = if (dataLength < count + limit) {
                    dataLength
                } else {
                    count + limit
                }

                val dataSplit = data.substring(start, end)

                count += dataSplit.length

                val phoneticMap = phoneticRepository.toPhonetics(dataSplit, it.code)

                val phoneticsOldMap = phoneticRepository.getPhonetics(phoneticMap.keys.toList()).associateBy {
                    it.text
                }

                phoneticMap.values.map { phonetic ->

                    val ipaOld = phoneticsOldMap[phonetic.text]?.ipa ?: phonetic.ipa

                    phonetic.ipa.putAll(ipaOld)

                    phonetic
                }

                phoneticRepository.insertOrUpdate(phoneticMap.values.toList())

                listState.put(it.name, State.SYNC_PHONETICS(it.name, count * 1f / dataLength))
                trySend(ResultState.Running(listState))
            }
        }


        val job = launch {

            for (i in 0..90) {

                listState.put("SYNC_TRANSLATE", State.SYNC_TRANSLATE("SYNC_TRANSLATE", i / 100f))
                trySend(ResultState.Running(listState))


                delay(200)
            }
        }

        runCatching {

            val languageOutput = languageRepository.getLanguageOutput()

            appRepository.translate(param.language.id, languageOutput.id, "hello")
        }.getOrElse {

        }

        job.cancel()

        listState.put("SYNC_TRANSLATE", State.SYNC_TRANSLATE("SYNC_TRANSLATE", 100f))
        trySend(ResultState.Running(listState))



        languageRepository.updateLanguageInput(param.language)


        listState.put("COMPLETED", State.COMPLETED)
        trySend(ResultState.Success(listState))


        awaitClose()
    }

    sealed class State(val value: Int) {

        data object START : State(0)

        data class SYNC_PHONETICS(val name: String, val percent: Float) : State(1)

        data class SYNC_TRANSLATE(val name: String, val percent: Float) : State(2)

        data object COMPLETED : State(Int.MAX_VALUE)
    }

    data class Param(
        val language: Language
    )
}