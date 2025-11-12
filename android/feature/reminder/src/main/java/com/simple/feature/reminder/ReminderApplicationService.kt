package com.simple.feature.reminder

import android.app.Application
import com.simple.autobind.annotation.AutoBind
import com.simple.service.ApplicationService

@AutoBind(ApplicationService::class)
class ReminderApplicationService : ApplicationService {

    override fun setup(application: Application) {
    }
}