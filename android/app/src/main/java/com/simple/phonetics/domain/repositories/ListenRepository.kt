package com.simple.phonetics.domain.repositories

import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow

interface ListenRepository {

    suspend fun getVoiceListSupportAsync(languageCode: String): ResultState<List<Int>>


    suspend fun startSpeakText(text: String, languageCode: String, voiceId: Int, voiceSpeed: Float): Flow<ResultState<String>>

    suspend fun stopSpeakText(): ResultState<String>
}