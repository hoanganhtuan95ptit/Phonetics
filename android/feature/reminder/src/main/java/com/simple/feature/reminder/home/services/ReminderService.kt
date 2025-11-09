package com.simple.feature.reminder.home.services

import android.annotation.SuppressLint
import com.simple.autobind.annotation.AutoBind
import com.simple.feature.reminder.data.cache.AppCache
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.services.HomeService

@AutoBind(HomeFragment::class)
class ReminderService : HomeService {

    @SuppressLint("ClickableViewAccessibility")
    override fun setup(homeFragment: HomeFragment) {

        homeFragment.view?.setOnTouchListener { _, _ ->

            AppCache.updateTimeUserInteractInHome()

            false
        }
    }
}