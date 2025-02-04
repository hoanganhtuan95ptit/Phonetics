package com.simple.phonetics.domain.repositories

import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow

interface SpeakRepository {

    suspend fun startSpeakText(languageCode: String): Flow<ResultState<String>>

    suspend fun stopSpeakText(): ResultState<String>
}