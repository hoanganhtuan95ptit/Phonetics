package com.simple.phonetics.domain.usecase

import android.util.Log
import com.simple.core.utils.extentions.toJson
import com.simple.detect.data.tasks.DetectStateTask
import com.simple.detect.entities.DetectProvider
import com.simple.detect.entities.DetectState
import com.simple.state.ResultState
import com.simple.state.toSuccess
import com.simple.task.executeAsyncAll
import kotlinx.coroutines.flow.firstOrNull

class DetectStateUseCase(
    private val detectStateTaskList: List<DetectStateTask>
) {

    suspend fun execute(param: Param): Boolean {

        val detectState = detectStateTaskList.executeAsyncAll(DetectStateTask.Param(languageCode = param.languageCode)).firstOrNull()

        val detectStateList = detectState?.toSuccess()?.data?.filterIsInstance<ResultState.Success<Pair<DetectProvider, DetectState>>>()?.map {

            it.data
        }

        return detectStateList?.any { it.second == DetectState.READY } == true
    }

    data class Param(
        val languageCode: String
    )
}