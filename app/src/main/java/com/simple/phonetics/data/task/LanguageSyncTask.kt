package com.simple.phonetics.data.task

import android.util.Log
import com.simple.core.utils.extentions.toJson
import com.simple.phonetics.domain.repositories.LanguageRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map

class LanguageSyncTask(
    private val languageRepository: LanguageRepository
) : SyncTask {

    override suspend fun executeTask(param: Unit) {

        channelFlow<Unit> {

            languageRepository.getLanguageOutputAsync().map {

                languageRepository.syncLanguageSupport(it.id)
            }.launchIn(this)

            awaitClose {

            }
        }.first()
    }
}