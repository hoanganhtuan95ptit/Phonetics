package com.simple.phonetics.domain.repositories

import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow

interface ReadingRepository {

    suspend fun getSupportedVoices(phoneticCode: String): ResultState<List<Int>>

    suspend fun startReading(text: String, phoneticCode: String, voiceId: Int, voiceSpeed: Float): Flow<ResultState<String>>

    suspend fun stopReading(): ResultState<String>
}