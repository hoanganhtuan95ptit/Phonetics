package com.simple.phonetics.domain.usecase

import com.simple.coreapp.utils.ext.launchCollect
import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class GetTranslateAsyncUseCase(
    private val appRepository: AppRepository,
    private val languageRepository: LanguageRepository,
) {

    suspend fun execute(): Flow<Map<String, String>> = channelFlow {

        // lấy bản dịch hiện có
        languageRepository.getLanguageOutputAsync().flatMapLatest {

            getKeyTranslateAsync(languageCode = it.id)
        }.launchCollect(this) {

            trySend(it)
        }

        awaitClose {

        }
    }

    private suspend fun getKeyTranslateAsync(languageCode: String) = appRepository.getTranslateAsync(languageCode = languageCode).map { keyTranslates ->

        val map = KeyTranslateMap()

        if (keyTranslates.isNotEmpty()) {

            map.putAll(keyTranslates)
        } else {

            map.putAll(appRepository.getKeyTranslateDefault())
        }

        map
    }

    private class KeyTranslateMap : HashMap<String, String>() {

        override fun get(key: String): String {
            return super.get(key) ?: key
        }
    }

    class Param()
}