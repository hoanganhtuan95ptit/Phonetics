package com.simple.feature.subscription.data.repositories

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ActivityTracker {
    // Sử dụng WeakReference để tránh rò rỉ bộ nhớ (Memory Leak)
    private val _currentActivity = MutableStateFlow<Activity?>(null)
    val currentActivity: StateFlow<Activity?> = _currentActivity.asStateFlow()

    fun setCurrentActivity(activity: Activity?) {
        _currentActivity.value = activity
    }
}