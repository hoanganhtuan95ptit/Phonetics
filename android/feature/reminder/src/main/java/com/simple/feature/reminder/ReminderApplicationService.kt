//package com.simple.feature.reminder
//
//import android.app.Activity
//import android.app.Application
//import android.os.Bundle
//import androidx.test.runner.lifecycle.ActivityLifecycleCallback
//import androidx.work.ExistingWorkPolicy
//import androidx.work.OneTimeWorkRequestBuilder
//import androidx.work.WorkManager
//import com.simple.autobind.annotation.AutoBind
//import com.simple.service.ApplicationService
//import java.util.Calendar
//import java.util.concurrent.TimeUnit
//
//@AutoBind(ApplicationService::class)
//class ReminderApplicationService : ApplicationService {
//
//    override suspend fun setup(application: Application) {
//
//        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks{
//
//            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
//            }
//
//            override fun onActivityStarted(activity: Activity) {
//            }
//
//            override fun onActivityResumed(activity: Activity) {
//            }
//
//            override fun onActivityPaused(activity: Activity) {
//            }
//
//            override fun onActivityStopped(activity: Activity) {
//            }
//
//            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
//            }
//
//            override fun onActivityDestroyed(activity: Activity) {
//            }
//        })
//
//        scheduleDailyCheck(application = application)
//    }
//
//    private fun scheduleDailyCheck(application: Application) {
//
//        // Tính thời gian đến 7:00 sáng tiếp theo
//        val now = Calendar.getInstance()
//        val next7AM = Calendar.getInstance().apply {
//            set(Calendar.HOUR_OF_DAY, 7)
//            set(Calendar.MINUTE, 0)
//            set(Calendar.SECOND, 0)
//            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
//        }
//
//        val delay = next7AM.timeInMillis - now.timeInMillis
//
//        // Work đầu tiên (chạy vào 7h sáng gần nhất)
//        val firstWork = OneTimeWorkRequestBuilder<MorningReminderWorker>()
//            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
//            .build()
//
//        WorkManager.getInstance(application).enqueueUniqueWork(
//            "morning_reminder_once",
//            ExistingWorkPolicy.REPLACE,
//            firstWork
//        )
//
//        // Khi hoàn thành, lên lịch lại cho 7h sáng hôm sau
//        WorkManager.getInstance(application).getWorkInfoByIdLiveData(firstWork.id).observeForever { info ->
//
//            if (info?.state?.isFinished == true) {
//                scheduleDailyCheck(application) // Đặt lại tự động cho ngày hôm sau
//            }
//        }
//    }
//}