package com.simple.phonetics.ui.main.services

import androidx.fragment.app.FragmentActivity
import com.simple.phonetics.ui.main.MainActivity
import com.simple.service.ActivityService

interface MainService : ActivityService {

    override fun setup(fragmentActivity: FragmentActivity) {

        if (fragmentActivity is MainActivity) setup(mainActivity = fragmentActivity)
    }

    fun setup(mainActivity: MainActivity)
}