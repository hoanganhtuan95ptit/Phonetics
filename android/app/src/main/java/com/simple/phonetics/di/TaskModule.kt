package com.simple.phonetics.di

import com.simple.phonetics.data.tasks.ConfigSyncTask
import com.simple.phonetics.data.tasks.EventSyncTask
import com.simple.phonetics.data.tasks.IpaSyncTask
import com.simple.phonetics.data.tasks.LanguageSyncTask
import com.simple.phonetics.data.tasks.PhoneticSyncTask
import com.simple.phonetics.data.tasks.TranslateSyncTask
import com.simple.phonetics.data.tasks.WordSyncTask
import com.simple.phonetics.domain.tasks.SyncTask
import org.koin.dsl.bind
import org.koin.dsl.module

@JvmField
val taskModule = module {

    single { WordSyncTask(get(), get(), get(), get()) } bind SyncTask::class

    single { IpaSyncTask(get(), get()) } bind SyncTask::class

    single { EventSyncTask(get(), get(), get()) } bind SyncTask::class

    single { ConfigSyncTask(get()) } bind SyncTask::class

    single { LanguageSyncTask(get()) } bind SyncTask::class

    single { PhoneticSyncTask() } bind SyncTask::class

    single { TranslateSyncTask(get(), get()) } bind SyncTask::class
}