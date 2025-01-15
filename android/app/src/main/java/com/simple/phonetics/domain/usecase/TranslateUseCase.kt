package com.simple.phonetics.domain.usecase

import com.simple.state.ResultState
import com.simple.task.executeSyncByPriority
import com.simple.translate.data.tasks.TranslateTask
import com.simple.translate.entities.TranslateRequest
import com.simple.translate.entities.TranslateResponse

class TranslateUseCase(
    private val listTranslateTask: List<TranslateTask>
) {

    suspend fun execute(param: Param): ResultState<List<TranslateResponse>> {

        val input = param.input.map {
            TranslateRequest(
                text = it,
                languageCode = param.inputLanguageCode
            )
        }

        val translateState = listTranslateTask.executeSyncByPriority(TranslateTask.Param(input = input, outputCode = param.outputLanguageCode))

        return translateState
    }

    class Param(
        val input: List<String>,
        val inputLanguageCode: String,
        val outputLanguageCode: String
    )
}