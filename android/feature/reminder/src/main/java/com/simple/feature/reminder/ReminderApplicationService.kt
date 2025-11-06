package com.simple.feature.reminder

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.simple.autobind.annotation.AutoBind
import com.simple.service.ApplicationService
import java.util.concurrent.TimeUnit

@AutoBind(ApplicationService::class)
class ReminderApplicationService : ApplicationService {

    override suspend fun setup(application: Application) {


        val workRequestBuilder = if (BuildConfig.DEBUG) {

            PeriodicWorkRequestBuilder<MorningReminderWorker>(25, TimeUnit.MINUTES)
        } else {

            PeriodicWorkRequestBuilder<MorningReminderWorker>(2, TimeUnit.HOURS)
        }

        val workRequest = workRequestBuilder.addTag("reminder_work")
            .build()

        WorkManager.getInstance(application)
            .enqueueUniquePeriodicWork("reminder_work", ExistingPeriodicWorkPolicy.UPDATE, workRequest)
    }
}