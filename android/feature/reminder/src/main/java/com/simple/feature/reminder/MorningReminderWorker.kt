//package com.simple.feature.reminder
//
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.PendingIntent
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import androidx.core.app.NotificationCompat
//import androidx.work.Worker
//import androidx.work.WorkerParameters
//import com.simple.phonetics.ui.MainActivity
//
//class MorningReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
//
//    override fun doWork(): Result {
//
//        val lastUsed = UserActivityTracker.getLastUsed(applicationContext)
//        if (lastUsed == 0L) return Result.success()
//
//        val daysSinceLastUse = (System.currentTimeMillis() - lastUsed) / (1000 * 60 * 60 * 24)
//
//        if (daysSinceLastUse >= 3) {
//            showNotification(
//                "Chào buổi sáng ☀️",
//                "Đã 3 ngày rồi bạn chưa mở ứng dụng. Chúc bạn một ngày tuyệt vời 🌼"
//            )
//        }
//
//        return Result.success()
//    }
//
//    private fun showNotification(title: String, message: String) {
//
//        val channelId = "morning_reminder_channel"
//        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                channelId,
//                "Reminder",
//                NotificationManager.IMPORTANCE_DEFAULT
//            )
//
//            manager.createNotificationChannel(channel)
//        }
//
//        val intent = Intent(applicationContext, MainActivity::class.java)
//        val pendingIntent = PendingIntent.getActivity(
//            applicationContext, 0, intent,
//            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
//        )
//
//        val notification = NotificationCompat.Builder(applicationContext, channelId)
//            .setSmallIcon(R.drawable.ic_notification)
//            .setContentTitle(title)
//            .setContentText(message)
//            .setContentIntent(pendingIntent)
//            .setAutoCancel(true)
//            .build()
//
//        manager.notify(2001, notification)
//    }
//}
