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
import java.text.Normalizer
import java.util.UUID

class GetPhoneticsAsyncUseCase(
    private val historyDao: HistoryDao,
    private val appRepository: AppRepository,
    private val phoneticRepository: PhoneticRepository
) {

    private val mapKeyAndSentence = hashMapOf<String, Sentence>()
    private val mapKeyAndPhonetic = hashMapOf<String, Phonetics>()


    private var id: String = ""
    private var textOld: String = ""

    suspend fun execute(param: Param?): Flow<ResultState<List<Any>>> = channelFlow {
        checkNotNull(param)


        val textNew = param.text.replace("  ", " ").trim().lowercase()

        if (textNew.isBlank()) {

            offerActiveAwait(ResultState.Success(emptyList()))
            return@channelFlow
        }


        offerActive(ResultState.Start)


        // tìm các trường tách dòng
        val lineDelimiters = getLineDelimiters(param)

        // tìm các trường tách chữ
        val wordDelimiters = getWordDelimiters(param)

        // nếu đang bật chế độ đảo ngược thì thực hiện dịch nội dung
        val textWrap = if (param.isReverse) {

            translate(textNew, param)
        } else {

            textNew
        }

        // lưu lịch sử tìm kiếm phiên âm
        val id = getId(textWrap)
        historyDao.insertOrUpdate(RoomHistory(id = id, text = textWrap))


        // tách dòng
        val list = textWrap.split(*lineDelimiters.toTypedArray()).mapIndexedNotNull { _, line ->

            if (line.isBlank()) {
                return@mapIndexedNotNull null
            }

            val sentenceObject = mapKeyAndSentence[line] ?: Sentence(line).apply {

                mapKeyAndSentence[line] = this
            }


            sentenceObject.phonetics = sentenceObject.text.split(*wordDelimiters.toTypedArray()).flatMap {

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

    private fun getId(textNew: String): String {

        val textNewNormalize = textNew.normalize()
        val textOldNormalize = textOld.lowercase()

        // thực hiện lấy id
        val id = historyDao.getRoomListByTextAsync(textNew).firstOrNull()?.id ?: if (textNewNormalize.startsWith(textOldNormalize) || textOldNormalize.startsWith(textNewNormalize)) {
            id
        } else {
            UUID.randomUUID().toString()
        }

        this@GetPhoneticsAsyncUseCase.id = id
        this@GetPhoneticsAsyncUseCase.textOld = textNew

        return id
    }

    private suspend fun translate(textNew: String, param: Param): String {

        val state = appRepository.translate(
            text = arrayOf(textNew),

            languageCodeInput = param.outputLanguageCode,
            languageCodeOutput = param.inputLanguageCode
        )

        return state.toSuccess()?.data?.firstOrNull()?.translateState?.toSuccess()?.data ?: textNew
    }

    private suspend fun getLineDelimiters(param: Param): List<String> {

        return arrayListOf(".", "!", "?", "\n")
    }

    private suspend fun getWordDelimiters(param: Param): List<String> {

        val wordDelimiters = arrayListOf(" ", "\n", ":")
        if (param.inputLanguageCode in listOf(Language.ZH, Language.JA, Language.KO)) {

            wordDelimiters.add("")
        }

        return wordDelimiters
    }

    private fun String.normalize() = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .replace(Regex("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsWhitespace}\\p{Punct}\\p{So}]"), "")

    data class Param(
        val text: String,

        val isReverse: Boolean,

        val inputLanguageCode: String,
        val outputLanguageCode: String
    )
}