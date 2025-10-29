package com.simple.phonetics.ui.home.services

import androidx.fragment.app.Fragment
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.service.FragmentService

interface HomeService : FragmentService {

    override suspend fun setup(fragment: Fragment) {

        if (fragment is HomeFragment) setup(homeFragment = fragment)
    }

    suspend fun setup(homeFragment: HomeFragment)
}