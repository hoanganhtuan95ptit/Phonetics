package com.simple.phonetics.ui.services

import androidx.fragment.app.FragmentActivity
import com.simple.phonetics.ui.MainActivity
import com.simple.service.ActivityService

interface MainService : ActivityService {

    override suspend fun setup(fragmentActivity: FragmentActivity) {

        if (fragmentActivity is MainActivity) setup(mainActivity = fragmentActivity)
    }

    fun setup(mainActivity: MainActivity)
}