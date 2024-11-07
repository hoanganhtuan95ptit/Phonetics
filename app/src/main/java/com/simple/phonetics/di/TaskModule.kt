package com.simple.phonetics.di

import com.simple.phonetics.data.task.ApiSyncTask
import com.simple.phonetics.data.task.DefaultSyncTask
import com.simple.phonetics.data.task.SyncTask
import org.koin.dsl.bind
import org.koin.dsl.module

@JvmField
val taskModule = module {

    single { ApiSyncTask(get(), get(), get()) } bind SyncTask::class

    single { DefaultSyncTask(get(), get(), get()) } bind SyncTask::class
}