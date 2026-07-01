package com.simple.phonetics.ui.home.services.language

import com.simple.autobind.annotation.AutoBind
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.services.HomeService
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.ui.precompute.image.setImage
import com.simple.ui.precompute.image.CircleCrop
import com.simple.ui.precompute.image.addTransform
import com.simple.ui.precompute.image.build
import com.simple.ui.precompute.image.toBuilder
import org.koin.androidx.viewmodel.ext.android.activityViewModel

@AutoBind(HomeFragment::class)
class LanguageHomeService() : HomeService {

    override fun setup(homeFragment: HomeFragment) {

        val configViewModel by homeFragment.activityViewModel<ConfigViewModel>()

        configViewModel.inputLanguage.collectWithLockTransitionUntilData(homeFragment, "HOME_LANGUAGE") {

            val binding = homeFragment.binding ?: return@collectWithLockTransitionUntilData

            binding.ivLanguage.setImage(
                it.image.toBuilder()
                    .addTransform(CircleCrop)
                    .build()
            )
        }


        val binding = homeFragment.binding ?: return

        binding.ivLanguage.setDebouncedClickListener {

            val transitionName = binding.ivLanguage.transitionName

            sendDeeplink(DeeplinkManager.LANGUAGE, extras = mapOf(Param.ROOT_TRANSITION_NAME to transitionName), sharedElement = mapOf(transitionName to binding.ivLanguage))
        }
    }
}
