package com.simple.phonetics.domain.repositories

import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow
import org.koin.core.context.GlobalContext

interface ReadingRepository {

    suspend fun getVoiceIdSelected(): Int

    suspend fun getVoiceIdSelectedAsync(): Flow<Int>

    suspend fun updateVoiceIdSelected(voiceId: Int)


    suspend fun getVoiceSpeed(): Float

    suspend fun getVoiceSpeedAsync(): Flow<Float>

    suspend fun updateVoiceSpeed(voiceSpeed: Float)


    suspend fun getSupportedVoices(phoneticCode: String): ResultState<List<Int>>

    suspend fun startReading(text: String, phoneticCode: String, voiceId: Int, voiceSpeed: Float): Flow<ResultState<String>>

    suspend fun stopReading(): ResultState<String>

    companion object {

        val instant by lazy {
            GlobalContext.get().get<ReadingRepository>()
        }
    }
}