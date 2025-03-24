package com.simple.phonetics.domain.usecase.language

import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.State
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

        listState["start"] = State.Start
        trySend(ResultState.Running(listState))


        param.language.listIpa.forEach { source ->

            val name = source.name
            val code = source.code

            listState[code] = State.SyncPhonetics(code = code, name = name, percent = 0f)
            trySend(ResultState.Running(listState))


            var count = 0

            val data = kotlin.runCatching {

                phoneticRepository.getSourcePhonetic(source)
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

                val phoneticMap = phoneticRepository.toPhonetics(dataSplit, source.code)

                val phoneticsOldMap = phoneticRepository.getPhonetics(phoneticMap.keys.toList()).associateBy {
                    it.text
                }

                phoneticMap.values.map { phonetic ->

                    val ipaOld = phoneticsOldMap[phonetic.text]?.ipa ?: phonetic.ipa

                    phonetic.ipa.putAll(ipaOld)

                    phonetic
                }

                phoneticRepository.insertOrUpdate(phoneticMap.values.toList())

                listState[code] = State.SyncPhonetics(code = code, name = name, percent = count * 1f / dataLength)
                trySend(ResultState.Running(listState))
            }
        }


        val job = launch {

            for (i in 0..90) {

                listState["SYNC_TRANSLATE"] = State.SyncTranslate("SYNC_TRANSLATE", i / 100f)
                trySend(ResultState.Running(listState))


                delay(200)
            }
        }

        runCatching {

            val languageOutput = languageRepository.getLanguageOutput()

            appRepository.translate(param.language.id, languageOutput.id, "hello")
        }

        job.cancel()

        listState["SYNC_TRANSLATE"] = State.SyncTranslate("SYNC_TRANSLATE", 100f)
        trySend(ResultState.Running(listState))


        languageRepository.updateLanguageInput(param.language)


        listState["COMPLETED"] = State.Completed
        trySend(ResultState.Success(listState))


        awaitClose()
    }

    data class Param(
        val language: Language
    )
}