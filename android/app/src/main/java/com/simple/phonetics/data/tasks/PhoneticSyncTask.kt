package com.simple.phonetics.data.tasks

import com.simple.phonetics.domain.repositories.PhoneticRepository
import com.simple.phonetics.domain.tasks.SyncTask
import org.koin.core.context.GlobalContext

class PhoneticSyncTask : SyncTask {

    override fun priority(): Int {

        return Int.MIN_VALUE
    }

    override suspend fun executeTask(param: SyncTask.Param) {

        GlobalContext.get().get<PhoneticRepository>().copy()
    }
}