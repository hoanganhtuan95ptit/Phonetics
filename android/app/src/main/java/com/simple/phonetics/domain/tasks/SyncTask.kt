package com.simple.phonetics.domain.tasks

import com.simple.phonetics.entities.Language
import com.simple.task.Task

interface SyncTask : Task<SyncTask.Param, Unit> {

    data class Param(val inputLanguage: Language, val outputLanguage: Language)
}