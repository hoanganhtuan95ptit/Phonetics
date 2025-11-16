package com.simple.phonetics.domain.repositories

import com.simple.state.ResultState
import com.tuanha.translate_2.entities.Translate
import kotlinx.coroutines.flow.Flow

interface TranslateRepository {

    suspend fun translateAwait(languageCodeInput: String, languageCodeOutput: String, vararg text: String): ResultState<List<Translate.Response>>

    suspend fun checkSupportTranslateAsync(languageCodeInput: String, languageCodeOutput: String): Flow<ResultState<Boolean>>
}