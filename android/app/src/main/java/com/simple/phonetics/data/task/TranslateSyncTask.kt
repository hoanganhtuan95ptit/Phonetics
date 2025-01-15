package com.simple.phonetics.data.task

import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map

class TranslateSyncTask(
    private val appRepository: AppRepository,
    private val languageRepository: LanguageRepository
) : SyncTask {

    override suspend fun executeTask(param: Unit) {

        channelFlow<Unit> {

            languageRepository.getLanguageOutputAsync().map {

                val list = appRepository.getKeyTranslate(it.id)

                appRepository.updateKeyTranslate(list)
            }.launchIn(this)

            awaitClose {

            }
        }.first()
    }
}