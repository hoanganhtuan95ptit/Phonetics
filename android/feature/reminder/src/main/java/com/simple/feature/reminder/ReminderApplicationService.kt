package com.simple.feature.reminder

import android.app.Application
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.simple.autobind.annotation.AutoBind
import com.simple.service.ApplicationService
import java.util.concurrent.TimeUnit

@AutoBind(ApplicationService::class)
class ReminderApplicationService : ApplicationService {

    override fun setup(application: Application) {


        Log.d("tuanha", "setup: ReminderApplicationService")
        val workRequestBuilder = if (BuildConfig.DEBUG) {

            PeriodicWorkRequestBuilder<MorningReminderWorker>(15, TimeUnit.MINUTES)
        } else {

            PeriodicWorkRequestBuilder<MorningReminderWorker>(2, TimeUnit.HOURS)
        }

        val workRequest = workRequestBuilder.addTag("reminder_work")
            .build()

        WorkManager.getInstance(application)
            .enqueueUniquePeriodicWork("reminder_work", ExistingPeriodicWorkPolicy.KEEP, workRequest)
    }
}