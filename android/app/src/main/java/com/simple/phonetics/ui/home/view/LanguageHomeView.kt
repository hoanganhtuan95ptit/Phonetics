package com.simple.phonetics.ui.home.view

import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.image.setImage
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.tuanha.deeplink.sendDeeplink
import org.koin.androidx.viewmodel.ext.android.activityViewModel

interface LanguageHomeView {

    fun setupLanguage(fragment: HomeFragment)
}

class LanguageHomeViewImpl() : LanguageHomeView {

    override fun setupLanguage(fragment: HomeFragment) {

        val configViewModel by fragment.activityViewModel<ConfigViewModel>()

        configViewModel.inputLanguage.collectWithLockTransitionUntilData(fragment, "HOME_LANGUAGE") {

            val binding = fragment.binding ?: return@collectWithLockTransitionUntilData

            binding.ivLanguage.setImage(it.image, CircleCrop())
        }

        val binding = fragment.binding ?: return

        binding.ivLanguage.setDebouncedClickListener {

            val transitionName = binding.ivLanguage.transitionName

            sendDeeplink(DeeplinkManager.LANGUAGE, extras = mapOf(Param.ROOT_TRANSITION_NAME to transitionName), sharedElement = mapOf(transitionName to binding.ivLanguage))
        }
    }

    companion object {

        private const val TAG_LANGUAGE = "TAG_LANGUAGE"
    }
}