package com.simple.phonetics.domain.repositories

import com.simple.state.ResultState
import kotlinx.coroutines.flow.Flow
import org.koin.core.context.GlobalContext

interface SpeakRepository {

    suspend fun checkSpeak(languageCode: String): Boolean

    suspend fun startSpeakText(languageCode: String): Flow<ResultState<String>>

    suspend fun stopSpeakText(): ResultState<String>

    companion object {

        val instant by lazy {
            GlobalContext.get().get<SpeakRepository>()
        }
    }
}