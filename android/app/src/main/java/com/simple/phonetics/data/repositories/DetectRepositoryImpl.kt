package com.simple.phonetics.data.repositories

import com.simple.detect_2.DetectTask
import com.simple.image.toBitmap
import com.simple.phonetics.domain.repositories.DetectRepository
import com.simple.startapp.StartApp
import com.simple.state.ResultState
import com.simple.state.map
import com.simple.state.mapToData
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

class DetectRepositoryImpl : DetectRepository {

    override suspend fun detectAwait(languageCodeInput: String, languageCodeOutput: String, path: String): ResultState<String> {

        val application = StartApp.applicationFlow.filterNotNull().first()

        val bitmap = path.toBitmap(application, width = 500, height = 500)

        return DetectTask.detectState(languageCode = languageCodeInput, source = bitmap).first().map { list ->

            list.joinToString("\n") { it.text }
        }
    }

    override suspend fun checkSupportDetectAsync(languageCodeInput: String, languageCodeOutput: String): Flow<ResultState<Boolean>> = channelFlow {

        trySend(ResultState.Start)

        DetectTask.isSupportState(languageCode = languageCodeInput).mapToData {

            true
        }.launchCollect(this) {

            trySend(it)
        }

        awaitClose {

        }
    }
}