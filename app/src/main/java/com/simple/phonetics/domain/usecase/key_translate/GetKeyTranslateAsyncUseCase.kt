package com.simple.phonetics.domain.usecase.key_translate

import com.simple.phonetics.domain.repositories.AppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class GetKeyTranslateAsyncUseCase(
    private val appRepository: AppRepository
) {

    suspend fun execute(): Flow<Map<String, String>> = appRepository.getLanguageCodeAsync().flatMapLatest {
        getKeyTranslateAsync(it)
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