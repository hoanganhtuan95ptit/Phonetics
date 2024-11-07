package com.simple.phonetics.domain.usecase

import com.simple.phonetics.data.task.SyncTask
import com.simple.task.executeAsyncAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn

class SyncUseCase(
    private val syncTaskList: List<SyncTask>
) {

    suspend fun execute() = channelFlow<Unit> {

        syncTaskList.executeAsyncAll(Unit).launchIn(this)

        awaitClose { }
    }
}