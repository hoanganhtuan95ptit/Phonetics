package com.simple.phonetics.di

import com.simple.phonetics.data.task.LanguageSyncTask
import com.simple.phonetics.data.task.SyncTask
import org.koin.dsl.bind
import org.koin.dsl.module

@JvmField
val taskModule = module {

    single { LanguageSyncTask(get()) } bind SyncTask::class
}