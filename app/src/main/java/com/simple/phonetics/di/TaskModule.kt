package com.simple.phonetics.di

import com.simple.analytics.Analytics
import com.simple.crashlytics.Crashlytics
import com.simple.phonetics.data.crashlytics.LogAnalytics
import com.simple.phonetics.data.crashlytics.LogCrashlytics
import com.simple.phonetics.data.task.ApiSyncTask
import com.simple.phonetics.data.task.SyncTask
import org.koin.dsl.bind
import org.koin.dsl.module

@JvmField
val taskModule = module {

    single { ApiSyncTask(get(), get(), get(), get()) } bind SyncTask::class

//    single { DefaultSyncTask(get(), get(), get()) } bind SyncTask::class


    single { LogAnalytics() } bind Analytics::class

    single { LogCrashlytics() } bind Crashlytics::class
}