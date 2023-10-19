package com.simple.phonetics.domain.usecase.phonetics

import com.simple.analytics.logAnalytics
import com.simple.core.utils.extentions.hasChar
import com.simple.core.utils.extentions.hasNumber
import com.simple.coreapp.data.usecase.BaseUseCase
import com.simple.coreapp.utils.extentions.offerActive
import com.simple.coreapp.utils.extentions.offerActiveAwait
import com.simple.phonetics.data.dao.PhoneticsDao
import com.simple.phonetics.data.dao.PhoneticsHistoryDao
import com.simple.phonetics.data.dao.RoomPhoneticHistory
import com.simple.phonetics.domain.entities.Phonetics
import com.simple.phonetics.domain.entities.Sentence
import com.simple.state.ResultState
import com.simple.state.isSuccess
import com.simple.state.toSuccess
import com.simple.task.executeAsyncByPriority
import com.simple.translate.TranslateTask
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.util.UUID

class GetPhoneticsAsyncUseCase(
    private val phoneticsDao: PhoneticsDao,
    private val phoneticsHistoryDao: PhoneticsHistoryDao,

    private val listTranslateTask: List<TranslateTask>
) : BaseUseCase<GetPhoneticsAsyncUseCase.Param, Flow<ResultState<List<Any>>>> {

    private val mapKeyAndSentence = hashMapOf<String, Sentence>()

    private val mapKeyAndPhonetic = hashMapOf<String, Phonetics>()


    private var id: String = UUID.randomUUID().toString()

    private var textBefore: String = ""


    override suspend fun execute(param: Param?): Flow<ResultState<List<Any>>> = channelFlow {
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


        phoneticsHistoryDao.getRoomListByTextAsync(textNew).firstOrNull()?.let {

            id = it.id
        }

        phoneticsHistoryDao.insertOrUpdate(RoomPhoneticHistory(id = id, text = textNew))


        textBefore = if (param.isReverse) {

            listTranslateTask.executeAsyncByPriority(TranslateTask.Param(listOf(textNew), param.outputLanguageCode, param.inputLanguageCode)).toSuccess()?.data?.firstOrNull() ?: textNew
        } else {

            textNew
        }


        val list = textBefore.split(".", "!", "?", "\n").mapIndexedNotNull { _, s ->

            val text = s.trim().replace(" ", " ").lowercase()

            if (text.isBlank()) {
                return@mapIndexedNotNull null
            }

            val sentenceObject = mapKeyAndSentence[text] ?: Sentence(text).apply {

                mapKeyAndSentence[text] = this
            }

            sentenceObject.phonetics = sentenceObject.text.split(" ", "\n").flatMap {

                if (it.endsWith(".")) listOf(it.substring(0, it.length - 1), ".")
                else if (it.endsWith(",")) listOf(it.substring(0, it.length - 1), ",")
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

            val mapTextAndPhonetics = phoneticsDao.getRoomListByTextList(mapKeyAndPhonetic.keys.toList()).associateBy { it.text.lowercase() }

            mapTextAndPhonetics.forEach {

                mapKeyAndPhonetic[it.key]?.ipa = it.value.ipa
            }

            offerActive(ResultState.Success(list))

            list.flatMap { it.phonetics }.forEach { phonetic ->

                if (phonetic.text.hasChar() && !phonetic.text.hasNumber() && (phonetic.ipa.isEmpty() || phonetic.ipa.toList().all { it.second.isEmpty() })) {

                    logAnalytics("IPA_EMPTY_${phonetic.text.uppercase()}" to phonetic.text)
                }
            }
        }

        launch {

            val mapKeyAndSentence = mapKeyAndSentence.filter { !it.value.translateState.isSuccess() }

            val state = listTranslateTask.executeAsyncByPriority(TranslateTask.Param(mapKeyAndSentence.keys.toList(), param.inputLanguageCode, param.outputLanguageCode))

            state.toSuccess()?.data?.forEachIndexed { index, s ->

                mapKeyAndSentence.toList().getOrNull(index)?.second?.translateState = ResultState.Success(s)
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
    ) : BaseUseCase.Param()
}