package com.simple.feature.reminder

import android.app.Application
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.simple.autobind.annotation.AutoBind
import com.simple.service.ApplicationService
import java.util.Calendar
import java.util.concurrent.TimeUnit

@AutoBind(ApplicationService::class)
class ReminderApplicationService : ApplicationService {

    override suspend fun setup(application: Application) {

        val now = Calendar.getInstance()

        val next7AM = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }

        val delay = next7AM.timeInMillis - System.currentTimeMillis()

        val workRequest = OneTimeWorkRequestBuilder<MorningReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("reminder_work")
            .build()

        WorkManager.getInstance(application)
            .enqueueUniqueWork("reminder_work", ExistingWorkPolicy.REPLACE, workRequest)
    }
}