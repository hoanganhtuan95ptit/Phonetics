package com.simple.feature.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.simple.feature.reminder.data.cache.AppCache
import com.simple.phonetics.domain.repositories.AppRepository
import com.simple.phonetics.domain.repositories.LanguageRepository
import com.simple.phonetics.ui.MainActivity
import kotlinx.coroutines.flow.first

class MorningReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        val lastUsed = AppCache.getTimeUserInteractInHome()

        if (lastUsed == 0L) {

            return Result.success()
        }

        val daysSinceLastUse = (System.currentTimeMillis() - lastUsed) / (1000 * 60 * 60 * 24)

        if (daysSinceLastUse >= 3) {

            randomNotification()
        }

        return Result.success()
    }

    private suspend fun randomNotification() {

        val titles = arrayOf(
            "reminder_title_1",
            "reminder_title_1",
            "reminder_title_1",
            "reminder_title_1",
            "reminder_title_1",
            "reminder_title_1",
            "reminder_title_1",
            "reminder_title_1",
            "reminder_title_1",
        )

        val messages = arrayOf(
            "reminder_message_1",
            "reminder_message_1",
            "reminder_message_1",
            "reminder_message_1",
            "reminder_message_1",
            "reminder_message_1",
            "reminder_message_1",
            "reminder_message_1",
            "reminder_message_1",
        )

        val languageCode = LanguageRepository.instant.getLanguageOutput().id

        val translateMap = AppRepository.instant.getTranslateAsync(keys = listOf(*titles, *messages), languageCode = languageCode).first()


        val titlesMap = messages.mapNotNull {
            translateMap[it]
        }

        val messagesWrap = messages.mapNotNull {
            translateMap[it]
        }


        if (titlesMap.isNotEmpty() && messagesWrap.isNotEmpty()) {

            showNotification(titlesMap.random(), messagesWrap.random())
        }
    }

    private fun showNotification(title: String, message: String) {

        val channelId = "morning_reminder_channel"
        val channelName = "Reminder"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)

            manager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(2001, notification)
    }
}
