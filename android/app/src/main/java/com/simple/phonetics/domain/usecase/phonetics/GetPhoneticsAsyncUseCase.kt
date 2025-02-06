package com.simple.phonetics.domain.usecase.phonetics

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

    private var id: String = ""
    private var textOld: String = ""

    suspend fun execute(param: Param?): Flow<ResultState<List<Any>>> = channelFlow {
        checkNotNull(param)


        val text = param.text.replace("  ", " ").trim().lowercase()

        if (text.isBlank()) {

            offerActiveAwait(ResultState.Success(emptyList()))
            return@channelFlow
        }


        offerActive(ResultState.Start)


        // nếu đang bật chế độ đảo ngược thì thực hiện dịch nội dung
        val textWrap = if (param.isReverse) {

            translate(text = text, languageCodeInput = param.outputLanguageCode, languageCodeOutput = param.inputLanguageCode)
        } else {

            text
        }

        // lưu lịch sử tìm kiếm phiên âm
        val id = getId(textWrap)
        historyDao.insertOrUpdate(RoomHistory(id = id, text = textWrap))


        // tìm các trường hợp tách dòng
        val lineDelimiters = getLineDelimiters()

        // tìm các trường hợp tách chữ
        val wordDelimiters = getWordDelimiters(param)

        // tách dòng
        val list = textWrap.split(*lineDelimiters.toTypedArray()).mapIndexedNotNull { _, line ->

            if (line.isBlank()) {
                return@mapIndexedNotNull null
            }

            val sentenceObject = Sentence(line)

            sentenceObject.phonetics = sentenceObject.text.split(*wordDelimiters.toTypedArray()).map {

                it.removeSpecialCharacters()
            }.filter {

                it.isNotBlank()
            }.map {

                Phonetics(it)
            }

            sentenceObject
        }


        offerActive(ResultState.Success(list))


        // tìm kiếm phiên âm
        launch {

            val phonetics = phoneticRepository.getPhonetics(list.flatMap { sentence -> sentence.phonetics.map { it.text } })

            val mapTextAndPhonetics = phonetics.associateBy { it.text }

            list.flatMap {

                it.phonetics
            }.forEach {

                it.ipa = mapTextAndPhonetics[it.text]?.ipa ?: hashMapOf()
            }

            offerActive(ResultState.Success(list))
        }

        // thực hiện dịch
        launch {

            list.forEach {

                it.translateState = translateState(text = it.text, languageCodeInput = param.inputLanguageCode, languageCodeOutput = param.outputLanguageCode)

                offerActive(ResultState.Success(list))
            }
        }


        awaitClose {}
    }

    /**
     * kiểm tra xem có cần tạo id mới không, và trả về id
     */
    private fun getId(textNew: String): String {

        val textNewNormalize = textNew.normalize()
        val textOldNormalize = textOld

        // thực hiện lấy id
        val id = historyDao.getRoomListByTextAsync(textNew).firstOrNull()?.id ?: if (textNewNormalize.startsWith(textOldNormalize) || textOldNormalize.startsWith(textNewNormalize)) {
            id
        } else {
            UUID.randomUUID().toString()
        }

        this@GetPhoneticsAsyncUseCase.id = id
        this@GetPhoneticsAsyncUseCase.textOld = textNewNormalize

        return id
    }

    /**
     * thực hiện dịch và trả về giá trị dịch
     */
    private suspend fun translate(text: String, languageCodeInput: String, languageCodeOutput: String): String {

        return translateState(text = text, languageCodeInput = languageCodeInput, languageCodeOutput = languageCodeOutput).toSuccess()?.data ?: text
    }

    /**
     * thực hiện dịch và trả về trạng thái dịch
     */
    private suspend fun translateState(text: String, languageCodeInput: String, languageCodeOutput: String): ResultState<String> {

        val state = appRepository.translate(
            text = arrayOf(text),

            languageCodeInput = languageCodeInput,
            languageCodeOutput = languageCodeOutput
        )

        return state.toSuccess()?.data?.firstOrNull()?.translateState ?: ResultState.Failed(RuntimeException(""))
    }

    private fun getLineDelimiters(): List<String> {

        return arrayListOf(".", "!", "?", "\n")
    }

    private fun getWordDelimiters(param: Param): List<String> {

        val wordDelimiters = arrayListOf(" ", "\n", ":")
        if (param.inputLanguageCode in listOf(Language.ZH, Language.JA, Language.KO)) {

            wordDelimiters.add("")
        }

        return wordDelimiters
    }

    private fun String.normalize() = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .replace(Regex("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsWhitespace}\\p{Punct}\\p{So}]"), "")

    // Regex để chỉ giữ lại chữ cái, số, khoảng trắng, và các ký tự Unicode khác
    private fun String.removeSpecialCharacters(): String = this
        .replace(Regex("[^\\p{L}\\p{N}\\p{Z}\\p{So}]"), "")

    data class Param(
        val text: String,

        val isReverse: Boolean,

        val inputLanguageCode: String,
        val outputLanguageCode: String
    )
}