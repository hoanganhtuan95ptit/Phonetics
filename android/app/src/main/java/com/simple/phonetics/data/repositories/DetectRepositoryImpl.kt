package com.simple.phonetics.data.repositories

import com.simple.detect_2.DetectTask
import com.simple.image.toBitmap
import com.simple.phonetics.domain.repositories.DetectRepository
import com.simple.startapp.StartApp
import com.simple.state.ResultState
import com.simple.state.runResultState
import com.simple.state.wrap
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull

class DetectRepositoryImpl : DetectRepository {

    override suspend fun detect(languageCodeInput: String, languageCodeOutput: String, path: String): ResultState<String> {

        val application = StartApp.applicationFlow.filterNotNull().first()

        val bitmap = path.toBitmap(application, width = 500, height = 500)

        return DetectTask.instant.mapNotNull { list ->

            list.find { it.isSupport(languageCodeInput) }
        }.first().runResultState {

            detect(bitmap)
        }.wrap {

            joinToString("\n") { it.text }
        }
    }

    override suspend fun isSupportDetect(languageCodeInput: String, languageCodeOutput: String): ResultState<Boolean> {

        val task = DetectTask.instant.mapNotNull { list -> list.find { it.isSupport(languageCodeInput) } }.firstOrNull()

        return ResultState.Success(true)
    }
}