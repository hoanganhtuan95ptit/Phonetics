package com.simple.phonetics.data.repositories

import com.simple.phonetics.domain.repositories.TranslateRepository
import com.simple.state.ResultState
import com.simple.state.isSuccess
import com.simple.state.runResultState
import com.simple.state.wrap
import com.tuanha.translate_2.TranslateTask
import com.tuanha.translate_2.entities.Translate
import kotlinx.coroutines.flow.first

class TranslateRepositoryImpl : TranslateRepository {

    override suspend fun translate(languageCodeInput: String, languageCodeOutput: String, vararg text: String): ResultState<List<Translate.Response>> {

        val input = text.map {

            Translate.Request(
                text = it,
                languageCode = languageCodeInput
            )
        }

        return TranslateTask.instant.first {

            it.isNotEmpty()
        }.first().runResultState {

            translate(input = input, outputLanguageCode = languageCodeOutput)
        }
    }

    override suspend fun isSupportTranslate(languageCodeInput: String, languageCodeOutput: String): ResultState<Boolean> {

        return translate(languageCodeInput = languageCodeInput, languageCodeOutput = languageCodeOutput, "hello").wrap {

            firstOrNull()?.state.isSuccess()
        }
    }
}