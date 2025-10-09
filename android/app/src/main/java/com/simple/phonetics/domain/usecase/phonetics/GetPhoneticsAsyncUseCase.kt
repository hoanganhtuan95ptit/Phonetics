package com.simple.phonetics.domain.usecase.phonetics

import com.simple.analytics.logAnalytics
import com.simple.coreapp.utils.extentions.offerActive
import com.simple.coreapp.utils.extentions.offerActiveAwait
import com.simple.phonetics.data.dao.HistoryDao
import com.simple.phonetics.data.dao.RoomHistory
import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.domain.repositories.WordRepository
import com.simple.phonetics.entities.Phonetic
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.entities.Word
import com.simple.phonetics.utils.exts.getLineDelimiters
import com.simple.phonetics.utils.exts.getWordDelimiters
import com.simple.phonetics.utils.exts.removeSpecialCharacters
import com.simple.state.ResultState
import com.simple.state.toSuccess
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.util.UUID

class GetPhoneticsAsyncUseCase(
    private val historyDao: HistoryDao,
    private val appRepository: AppRepository,
    private val wordRepository: WordRepository,
    private val phoneticRepository: PhoneticRepository
) {

    private var id: String = ""

    suspend fun execute(param: Param?): Flow<ResultState<List<Any>>> = channelFlow {
        checkNotNull(param)


        val text = param.textNew.replace("  ", " ").trim().lowercase()

        if (text.isBlank()) {

            offerActiveAwait(ResultState.Success(emptyList()))
            return@channelFlow
        }


        if (param.textOld.isBlank()) {

            offerActive(ResultState.Start)
        }

        // nếu đang bật chế độ đảo ngược thì thực hiện dịch nội dung
        val textWrap = if (param.isReverse) {

            translate(text = text, languageCodeInput = param.outputLanguageCode, languageCodeOutput = param.inputLanguageCode)
        } else {

            text
        }

        // lưu lịch sử tìm kiếm phiên âm
        if (param.saveToHistory) {

            val id = getId(textOld = param.textOld, textNew = textWrap)

            historyDao.insertOrUpdate(RoomHistory(id = id, text = textWrap))
        }


        // tìm các trường hợp tách dòng
        val lineDelimiters = getLineDelimiters()

        // tìm các trường hợp tách chữ
        val wordDelimiters = getWordDelimiters(languageCode = param.inputLanguageCode)

        // tách dòng
        val sentenceList = textWrap.split(*lineDelimiters.toTypedArray()).mapIndexedNotNull { _, line ->

            if (line.isBlank()) {
                return@mapIndexedNotNull null
            }

            val sentenceObject = Sentence(line)

            val wordList = sentenceObject.text.split(*wordDelimiters.toTypedArray()).map {

                it.lowercase().removeSpecialCharacters()
            }.filter {

                it.isNotBlank()
            }

            sentenceObject.phonetics = wordList.map {

                Phonetic(text = it)
            }

            sentenceObject
        }


        offerActive(ResultState.Success(sentenceList))


        // tìm kiếm phiên âm
        val ipaJob = launch {

            val wordList = sentenceList.flatMap { sentence -> sentence.phonetics.map { it.text } }

            val mapTextAndPhonetics = phoneticRepository.getPhonetics(
                textList = wordList,
                phoneticCode = param.phoneticCode
            ).associateBy {
                it.text.lowercase()
            }

            sentenceList.flatMap {

                it.phonetics
            }.forEach {

                it.ipa = mapTextAndPhonetics[it.text]?.ipa ?: hashMapOf()
            }

            offerActive(ResultState.Success(sentenceList))
        }

        // thực hiện dịch
        launch {

            sentenceList.forEach {

                it.translateState = translateState(text = it.text, languageCodeInput = param.inputLanguageCode, languageCodeOutput = param.outputLanguageCode)

                offerActive(ResultState.Success(sentenceList))
            }
        }

        // thực hiện lưu những từ đã tra phiên âm
        if (param.saveToHistory) launch {

            // đợi cho luồng lấy IPA hoàn thành thì mới lưu vào db
            ipaJob.join()

            // lấy ra những từ có IPA
            val wordList = sentenceList.flatMap {
                it.phonetics
            }.filter {
                it.ipa[param.phoneticCode].orEmpty().isNotEmpty()
            }.map {
                it.text
            }

            wordRepository.insertOrUpdate(resource = Word.Resource.History.value, languageCode = param.inputLanguageCode, wordList)
        }

        logAnalytics("search_phonetics_${param.inputLanguageCode.lowercase()}")

        awaitClose {}
    }

    /**
     * kiểm tra xem có cần tạo id mới không, và trả về id
     */
    private fun getId(textOld: String, textNew: String): String {

        // thực hiện lấy id
        val id = historyDao.getRoomListByTextAsync(textNew).firstOrNull()?.id ?: if (textOld.isBlank()) {
            UUID.randomUUID().toString()
        } else {
            id
        }

        this@GetPhoneticsAsyncUseCase.id = id

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

        val state = appRepository.translateAsync(
            text = arrayOf(text),

            languageCodeInput = languageCodeInput,
            languageCodeOutput = languageCodeOutput
        )

        return state.toSuccess()?.data?.firstOrNull()?.state ?: ResultState.Failed(RuntimeException(""))
    }

    data class Param(
        val textOld: String = "",
        val textNew: String = "",

        val isReverse: Boolean,

        val phoneticCode: String,
        val inputLanguageCode: String,
        val outputLanguageCode: String,

        val saveToHistory: Boolean = true
    )
}