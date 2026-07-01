package com.simple.feature.subscription

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.simple.autobind.annotation.AutoBind
import com.simple.feature.subscription.data.repositories.ActivityTracker
import com.simple.service.ApplicationService

@AutoBind(ApplicationService::class)
class SubscriptionApplicationService : ApplicationService {

    override fun setup(application: Application) {

        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {

            override fun onActivityResumed(activity: Activity) {

                // Khi Activity hiện lên trên cùng, cập nhật Flow
                ActivityTracker.setCurrentActivity(activity)
            }

            override fun onActivityPaused(activity: Activity) {

                // Khi Activity không còn tương tác, có thể set null
                // hoặc giữ nguyên tùy vào mục đích của bạn
                if (ActivityTracker.currentActivity.value == activity) {
                    ActivityTracker.setCurrentActivity(null)
                }
            }

            // Các hàm khác không dùng đến thì để trống
            override fun onActivityCreated(p0: Activity, p1: Bundle?) {}
            override fun onActivityStarted(p0: Activity) {}
            override fun onActivityStopped(p0: Activity) {}
            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}
            override fun onActivityDestroyed(p0: Activity) {}
        })
    }
}