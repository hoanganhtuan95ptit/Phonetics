package com.simple.phonetics.ui.phonetics.view

import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.image.setImage
import com.simple.phonetics.Deeplink
import com.simple.phonetics.Param
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.phonetics.PhoneticsFragment
import com.simple.phonetics.ui.phonetics.PhoneticsViewModel
import com.simple.phonetics.utils.sendDeeplink
import org.koin.androidx.viewmodel.ext.android.activityViewModel

interface LanguageView {

    fun setupLanguage(fragment: PhoneticsFragment)
}

class LanguageViewImpl() : LanguageView {

    override fun setupLanguage(fragment: PhoneticsFragment) {

        fragment.lockTransition(TAG_LANGUAGE)

        val viewModel by fragment.viewModels<PhoneticsViewModel>()
        val configViewModel by fragment.activityViewModel<ConfigViewModel>()

        configViewModel.inputLanguage.observe(fragment.viewLifecycleOwner) {

            viewModel.updateInputLanguage(it)

            val binding = fragment.binding ?: return@observe

            binding.ivLanguage.setImage(it.image, CircleCrop())

            fragment.unlockTransition(TAG_LANGUAGE)
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