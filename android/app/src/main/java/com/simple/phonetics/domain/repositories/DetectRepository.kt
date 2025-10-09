package com.simple.phonetics.domain.repositories

import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow

interface DetectRepository {

    suspend fun detectAsync(languageCodeInput: String, languageCodeOutput: String, path: String): ResultState<String>

    suspend fun checkSupportDetectAsync(languageCodeInput: String, languageCodeOutput: String): Flow<ResultState<Boolean>>
}