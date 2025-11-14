package com.simple.feature.reminder.services

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.simple.autobind.annotation.AutoBind
import com.simple.event.listenerEvent
import com.simple.feature.reminder.MorningReminderWorker
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.services.MainService
import com.simple.phonetics.ui.services.queue.QueueEventState
import com.simple.state.ResultState
import java.util.concurrent.TimeUnit

private const val tag = "REMINDER"
private const val order = 10

@AutoBind(MainActivity::class)
class ReminderService : MainService {

    override fun setup(mainActivity: MainActivity) {

        setupJob(mainActivity)
        setupConfirm(mainActivity)
    }

    private fun setupJob(mainActivity: MainActivity) {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // chấp nhận mọi loại mạng
            .setRequiresBatteryNotLow(false)                 // chấp nhận pin yếu
            .setRequiresCharging(false)                      // không cần đang sạc
            .setRequiresStorageNotLow(false)                 // chấp nhận bộ nhớ thấp
            .build()

        val workRequest = PeriodicWorkRequestBuilder<MorningReminderWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(mainActivity).cancelUniqueWork("reminder_work")
        WorkManager.getInstance(mainActivity).enqueueUniquePeriodicWork("reminder_work", ExistingPeriodicWorkPolicy.REPLACE, workRequest)
    }

    private fun setupConfirm(mainActivity: MainActivity) {

        QueueEventState.addTag(tag = tag, order = order)

        val requestNotificationPermission = mainActivity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->

            QueueEventState.endTag(tag = tag, order = order, success = isGranted)
        }


        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

            ResultState.Running(Unit)
        } else {

            ResultState.Success(Unit)
        }

        QueueEventState.updateState(tag, order = order, state = state)

        listenerEvent(mainActivity.lifecycle, eventName = tag) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}