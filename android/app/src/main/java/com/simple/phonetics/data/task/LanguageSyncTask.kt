package com.simple.phonetics.data.task

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

            val languageInput = languageRepository.getLanguageInput()

            languageRepository.getLanguageOutputAsync().map {

                val languageList = languageRepository.syncLanguageSupport(it.id)

                languageList.find {
                    it.id == languageInput?.id
                }?.let {
                    languageRepository.updateLanguageInput(it)
                }

            }.launchIn(this)

            awaitClose {

            }
        }.first()
    }
}