package com.simple.phonetics.domain.usecase.phonetics

import com.simple.analytics.logAnalytics
import com.simple.core.utils.extentions.hasChar
import com.simple.core.utils.extentions.hasNumber
import com.simple.coreapp.utils.extentions.offerActive
import com.simple.coreapp.utils.extentions.offerActiveAwait
import com.simple.phonetics.data.dao.HistoryDao
import com.simple.phonetics.data.dao.RoomHistory
import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.entities.Language
import com.simple.phonetics.entities.Phonetics
import com.simple.phonetics.entities.Sentence
import com.simple.state.ResultState
import com.simple.state.isSuccess
import com.simple.state.toSuccess
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.util.UUID

class GetPhoneticsAsyncUseCase(
    private val historyDao: HistoryDao,
    private val appRepository: AppRepository,
    private val phoneticRepository: PhoneticRepository
) {

    private val mapKeyAndSentence = hashMapOf<String, Sentence>()

    private val mapKeyAndPhonetic = hashMapOf<String, Phonetics>()


    private var id: String = UUID.randomUUID().toString()

    private var textBefore: String = ""


    suspend fun execute(param: Param?): Flow<ResultState<List<Any>>> = channelFlow {
        checkNotNull(param)


        val textNew = param.text.trim()

        if (textNew.isBlank()) {

            id = UUID.randomUUID().toString()
            offerActiveAwait(ResultState.Success(emptyList()))
            return@channelFlow
        }

        if (textBefore.isBlank() || (!textBefore.startsWith(textNew) && !textNew.startsWith(textBefore))) {

            offerActive(ResultState.Start)
        }


        historyDao.getRoomListByTextAsync(textNew).firstOrNull()?.let {

            id = it.id
        }

        historyDao.insertOrUpdate(RoomHistory(id = id, text = textNew))


        textBefore = if (param.isReverse) {

            val state = appRepository.translate(
                languageCodeInput = param.outputLanguageCode,
                languageCodeOutput = param.inputLanguageCode,
                text = arrayOf(textNew)
            )

            state.toSuccess()?.data?.firstOrNull()?.translateState?.toSuccess()?.data ?: textNew
        } else {

            textNew
        }


        val list = textBefore.split(".", "!", "?", "\n").mapIndexedNotNull { _, s ->

            val text = s.trim().replace("  ", " ").lowercase()

            if (text.isBlank()) {
                return@mapIndexedNotNull null
            }

            val sentenceObject = mapKeyAndSentence[text] ?: Sentence(text).apply {

                mapKeyAndSentence[text] = this
            }


            // tách chữ
            val delimiters = arrayListOf(" ", "\n", ":")

            if (param.inputLanguageCode in listOf(Language.ZH, Language.JA, Language.KO)) {

                delimiters.add("")
            }

            sentenceObject.phonetics = sentenceObject.text.split(*delimiters.toTypedArray()).flatMap {

                if (it.endsWith(".")) listOf(it.substring(0, it.length - 1), ".")
                else if (it.endsWith(",")) listOf(it.substring(0, it.length - 1), ",")
                else if (it.endsWith("\"")) listOf(it.substring(0, it.length - 1), "\"")
                else listOf(it)
            }.map {

                val word = it.lowercase()

                mapKeyAndPhonetic[word] ?: Phonetics(it).apply {

                    mapKeyAndPhonetic[word] = this
                }
            }

            sentenceObject
        }

        offerActive(ResultState.Success(list))


        launch {

            val mapKeyAndPhonetic = mapKeyAndPhonetic.filter { it.value.ipa.isEmpty() }

            val mapTextAndPhonetics = phoneticRepository.getPhonetics(mapKeyAndPhonetic.keys.toList()).associateBy { it.text.lowercase() }

            mapTextAndPhonetics.forEach {

                mapKeyAndPhonetic[it.key]?.ipa = it.value.ipa
            }

            offerActive(ResultState.Success(list))

            list.flatMap { it.phonetics }.forEach { phonetic ->

                if (phonetic.text.hasChar() && !phonetic.text.hasNumber() && (phonetic.ipa.isEmpty() || phonetic.ipa.toList().all { it.second.isEmpty() })) {

                    logAnalytics("IPA_EMPTY", "code" to phonetic.text)
                }
            }
        }

        launch {

            val mapKeyAndSentence = mapKeyAndSentence.filter { !it.value.translateState.isSuccess() }

            val state = appRepository.translate(
                languageCodeInput = param.inputLanguageCode,
                languageCodeOutput = param.outputLanguageCode,
                text = mapKeyAndSentence.keys.toTypedArray()
            )

            state.toSuccess()?.data?.forEachIndexed { index, s ->

                mapKeyAndSentence.toList().getOrNull(index)?.second?.translateState = s.translateState
            }

            offerActive(ResultState.Success(list))
        }


        awaitClose {}
    }

    data class Param(
        val text: String,

        val isReverse: Boolean,

        val inputLanguageCode: String,
        val outputLanguageCode: String
    )
}