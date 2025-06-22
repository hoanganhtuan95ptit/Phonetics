package com.simple.phonetics.domain.usecase.speak

import com.simple.coreapp.utils.ext.launchCollect
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.domain.repositories.SpeakRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

class CheckSupportSpeakAsyncUseCase(
    private val speakRepository: SpeakRepository,
    private val languageRepository: LanguageRepository
) {

    suspend fun execute(): Flow<Boolean> = channelFlow {

        languageRepository.getLanguageInputAsync().launchCollect(this) {

            trySend(speakRepository.checkSpeak(it.id))
        }

        awaitClose {

        }
    }
}