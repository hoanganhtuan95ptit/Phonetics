package com.simple.phonetics.ui.home.services

import androidx.fragment.app.Fragment
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.service.FragmentViewCreatedService

interface HomeService : FragmentViewCreatedService {

    override fun setup(fragment: Fragment) {

        if (fragment is HomeFragment) setup(homeFragment = fragment)
    }

    fun setup(homeFragment: HomeFragment)
}