package com.simple.feature.subscription

import androidx.fragment.app.FragmentActivity
import com.simple.autobind.annotation.AutoBind
import com.simple.feature.subscription.data.repositories.ActivityTracker
import com.simple.service.ActivityService

@AutoBind(ActivityService::class)
class SubscriptionActivityService : ActivityService {

    override fun setup(fragmentActivity: FragmentActivity) {

        ActivityTracker.setCurrentActivity(fragmentActivity)
    }
}