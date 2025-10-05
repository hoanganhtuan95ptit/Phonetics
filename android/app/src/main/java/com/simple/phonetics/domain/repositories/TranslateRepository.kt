package com.simple.phonetics.domain.repositories

import com.simple.state.ResultState
import com.tuanha.translate_2.entities.Translate

interface TranslateRepository {

    suspend fun translate(languageCodeInput: String, languageCodeOutput: String, vararg text: String): ResultState<List<Translate.Response>>

    suspend fun isSupportTranslate(languageCodeInput: String, languageCodeOutput: String): ResultState<Boolean>
}