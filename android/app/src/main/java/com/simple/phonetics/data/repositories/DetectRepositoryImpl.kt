package com.simple.phonetics.data.repositories

import com.simple.detect_2.DetectTask
import com.simple.image.toBitmap
import com.simple.phonetics.domain.repositories.DetectRepository
import com.simple.startapp.StartApp
import com.simple.state.ResultState
import com.simple.state.map
import com.simple.state.runResultState
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class DetectRepositoryImpl : DetectRepository {

    override suspend fun detectAsync(languageCodeInput: String, languageCodeOutput: String, path: String): ResultState<String> {

        val application = StartApp.applicationFlow.filterNotNull().first()

        val bitmap = path.toBitmap(application, width = 500, height = 500)

        return DetectTask.instant.mapNotNull { list ->

            list.find { it.isSupport(languageCodeInput) }
        }.first().runResultState {

            detect(bitmap)
        }.map { list ->

            list.joinToString("\n") { it.text }
        }
    }

    override suspend fun checkSupportDetectAsync(languageCodeInput: String, languageCodeOutput: String): Flow<ResultState<Boolean>> = channelFlow {

        trySend(ResultState.Start)

        DetectTask.instant.map { list ->

            list.find { it.isSupport(languageCodeInput) }
        }.launchCollect(this) {

            trySend(ResultState.Success(it != null))
        }

        awaitClose {

        }
    }
}