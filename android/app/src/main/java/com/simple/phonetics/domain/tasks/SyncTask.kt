package com.simple.phonetics.domain.tasks

import com.simple.task.Task

interface SyncTask : Task<SyncTask.Param, Unit> {

    class Param()
}