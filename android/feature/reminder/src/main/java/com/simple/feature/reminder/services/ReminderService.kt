package com.simple.feature.reminder.services

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.simple.autobind.annotation.AutoBind
import com.simple.event.listenerEvent
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.services.MainService
import com.simple.phonetics.ui.services.queue.QueueEventState
import com.simple.state.ResultState

private const val tag = "REMINDER"
private const val order = 10

@AutoBind(MainActivity::class)
class ReminderService : MainService {

    override fun setup(mainActivity: MainActivity) {

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