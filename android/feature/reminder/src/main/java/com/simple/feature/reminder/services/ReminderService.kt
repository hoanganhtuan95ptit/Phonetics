package com.simple.feature.reminder.services

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.simple.autobind.annotation.AutoBind
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.services.MainService
import com.simple.phonetics.ui.services.queue.QueueEventState

private const val tag = "REMINDER"
private const val order = 10
private const val KEY_REQUEST = "${tag}_KEY_REQUEST"

@AutoBind(MainActivity::class)
class ReminderService : MainService {

    override fun setup(mainActivity: MainActivity) {

        QueueEventState.addTag(tag = tag, order = order)

        val requestNotificationPermission = mainActivity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->

            QueueEventState.endTag(tag = tag, order = order, success = isGranted)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

            Log.d("tuanha", "setup: ReminderService")
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {

            QueueEventState.endTag(tag, order = order)
        }
    }
}