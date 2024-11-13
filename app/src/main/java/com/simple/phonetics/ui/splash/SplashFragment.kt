package com.simple.phonetics.ui.splash

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.simple.phonetics.Deeplink
import com.simple.phonetics.databinding.FragmentSplashBinding
import com.simple.phonetics.ui.base.TransitionFragment
import com.simple.phonetics.utils.sendDeeplink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : TransitionFragment<FragmentSplashBinding, SplashViewModel>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {

            delay(1000)

            sendDeeplink(Deeplink.LANGUAGE)
        }
    }
}