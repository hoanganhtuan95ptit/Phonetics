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

        // đông bộ dự liệu translate cú sang bảng mới
        copyTranslate()

        // call api để lấy bản dịch
        languageRepository.getLanguageOutputAsync().launchCollect(this) {

            runCatching {

                val languageCode = it.id

                val map = appRepository.syncTranslate(languageCode = languageCode)

                appRepository.updateTranslate(languageCode = languageCode, map = map)
            }
        }

        // lấy bản dịch hiện có
        languageRepository.getLanguageOutputAsync().flatMapLatest {

            getKeyTranslateAsync(languageCode = it.id)
        }.launchCollect(this) {

            trySend(it)
        }

        awaitClose {

        }
    }

    private suspend fun copyTranslate() {

        if (appRepository.getCountTranslate() > 0) return

        appRepository.getAllTranslateOld().groupBy { it.langCode }.mapValues { entry ->

            entry.value.map { it.key to it.value }.toMap()
        }.map {

            appRepository.updateTranslate(languageCode = it.key, map = it.value)
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