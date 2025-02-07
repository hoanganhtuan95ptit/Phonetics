package com.simple.phonetics.domain.usecase

import com.simple.coreapp.utils.ext.launchCollect
import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.entities.KeyTranslate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map

class GetKeyTranslateAsyncUseCase(
    private val appRepository: AppRepository,
    private val languageRepository: LanguageRepository,
) {

    suspend fun execute(): Flow<Map<String, String>> = channelFlow {

        // lấy bản dịch hiện có
        languageRepository.getLanguageOutputAsync().flatMapLatest {

            getKeyTranslateAsync(langCode = it.id)
        }.launchCollect(this) {

            trySend(it)
        }

        // call api để lấy bản dịch
        languageRepository.getLanguageOutputAsync().distinctUntilChanged().map {

            val list = appRepository.runCatching { getKeyTranslate(it.id) }.getOrNull() ?: return@map emptyList<KeyTranslate>()

            appRepository.updateKeyTranslate(list)
        }.launchIn(this)

        awaitClose {

        }
    }

    private suspend fun getKeyTranslateAsync(langCode: String) = appRepository.getKeyTranslateAsync(langCode).map { keyTranslates ->

        val map = KeyTranslateMap()

        if (keyTranslates.isNotEmpty()) keyTranslates.forEach {

            map[it.key] = it.value
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