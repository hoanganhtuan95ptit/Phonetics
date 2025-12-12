package com.simple.phonetics.ui.speak.services

import androidx.fragment.app.Fragment
import com.simple.phonetics.ui.speak.SpeakFragment
import com.simple.service.FragmentViewCreatedService

interface SpeakService : FragmentViewCreatedService {

    override fun setup(fragment: Fragment) {

        if (fragment is SpeakFragment) setup(fragment)
    }

    fun setup(fragment: SpeakFragment)
}