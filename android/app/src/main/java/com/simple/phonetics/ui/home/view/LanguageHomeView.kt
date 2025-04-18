package com.simple.phonetics.ui.home.view

import androidx.core.os.bundleOf
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.image.setImage
import com.simple.phonetics.Deeplink
import com.simple.phonetics.Param
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.sendDeeplink
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

            sendDeeplink(Deeplink.LANGUAGE, extras = bundleOf(Param.ROOT_TRANSITION_NAME to transitionName), sharedElement = mapOf(transitionName to binding.ivLanguage))
        }
    }

    companion object {

        private const val TAG_LANGUAGE = "TAG_LANGUAGE"
    }
}