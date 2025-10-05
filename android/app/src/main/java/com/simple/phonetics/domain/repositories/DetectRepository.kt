package com.simple.phonetics.domain.repositories

import com.simple.state.ResultState

interface DetectRepository {

    suspend fun detect(languageCodeInput: String, languageCodeOutput: String, path: String): ResultState<String>

    suspend fun isSupportDetect(languageCodeInput: String, languageCodeOutput: String): ResultState<Boolean>
}