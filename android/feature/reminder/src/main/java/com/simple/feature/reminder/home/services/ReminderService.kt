package com.simple.feature.reminder.home.services

import android.annotation.SuppressLint
import androidx.lifecycle.lifecycleScope
import com.simple.autobind.annotation.AutoBind
import com.simple.feature.reminder.data.cache.AppCache
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.services.HomeService
import com.unknown.coroutines.handler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AutoBind(HomeFragment::class)
class ReminderService : HomeService {

    @SuppressLint("ClickableViewAccessibility")
    override fun setup(homeFragment: HomeFragment) {

        var job: Job? = null

        homeFragment.binding?.root?.setOnInterceptTouchListener {

            job?.cancel()
            job = homeFragment.viewLifecycleOwner.lifecycleScope.launch(handler + Dispatchers.IO) {

                delay(350)
                AppCache.updateTimeUserInteractInHome()
            }

            false
        }
    }
}