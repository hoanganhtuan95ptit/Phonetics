package com.simple.phonetics.domain.usecase

import com.simple.coreapp.data.usecase.BaseUseCase
import com.simple.task.executeAsyncAll
import com.simple.phonetics.data.task.SyncTask
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn

class SyncUseCase(
    private val syncTaskList: List<SyncTask>
) : BaseUseCase<BaseUseCase.Param, Flow<Unit>> {

    override suspend fun execute(param: BaseUseCase.Param?) = channelFlow<Unit> {

        syncTaskList.executeAsyncAll(Unit).launchIn(this)

        awaitClose { }
    }
}