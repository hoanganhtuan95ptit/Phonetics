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

    override fun setup(application: Application) {

        val workRequestBuilder = PeriodicWorkRequestBuilder<MorningReminderWorker>(1, TimeUnit.HOURS)

        val workRequest = workRequestBuilder.addTag("reminder_work")
            .build()

        WorkManager.getInstance(application)
            .enqueueUniquePeriodicWork("reminder_work", ExistingPeriodicWorkPolicy.KEEP, workRequest)
    }
}