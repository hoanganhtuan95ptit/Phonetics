package com.simple.phonetics.ui.home.services.microphone

import com.simple.autobind.annotation.AutoBind
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.deeplink.sendDeeplink
import com.simple.event.listenerEvent
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.EventName
import com.simple.phonetics.Param
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.services.HomeService
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

@AutoBind(HomeFragment::class)
class MicrophoneHomeService : HomeService {

    override fun setup(homeFragment: HomeFragment) {

        val viewModel: HomeViewModel by homeFragment.viewModel()

        val configViewModel: ConfigViewModel by homeFragment.activityViewModel()

        val microphoneHomeViewModel: MicrophoneHomeViewModel by homeFragment.viewModel()


        homeFragment.binding?.ivMicrophone?.setDebouncedClickListener {

            sendDeeplink(DeeplinkManager.RECORDING, extras = mapOf(Param.REVERSE to viewModel.isReverse.value, Param.KEY_REQUEST to EventName.MICROPHONE))
        }

        listenerEvent(lifecycle = homeFragment.viewLifecycleOwner.lifecycle, eventName = EventName.MICROPHONE) {

            val result = it.asObjectOrNull<String>() ?: return@listenerEvent
            val binding = homeFragment.binding ?: return@listenerEvent

            viewModel.getPhonetics("")
            binding.etText.setText(result)
        }

        observeData(fragment = homeFragment, viewModel = viewModel, configViewModel = configViewModel, microphoneHomeViewModel = microphoneHomeViewModel)
        observeMicrophoneData(fragment = homeFragment, viewModel = viewModel, configViewModel = configViewModel, microphoneHomeViewModel = microphoneHomeViewModel)
    }

    private fun observeData(fragment: HomeFragment, viewModel: HomeViewModel, configViewModel: ConfigViewModel, microphoneHomeViewModel: MicrophoneHomeViewModel) = with(viewModel) {

        val viewLifecycleOwner = fragment.viewLifecycleOwner

        isReverse.observe(viewLifecycleOwner) {

            microphoneHomeViewModel.updateReverse(it)
        }
    }

    private fun observeMicrophoneData(fragment: HomeFragment, viewModel: HomeViewModel, configViewModel: ConfigViewModel, microphoneHomeViewModel: MicrophoneHomeViewModel) = with(microphoneHomeViewModel) {

        microphoneInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "MICROPHONE") {

            val binding = fragment.binding ?: return@collectWithLockTransitionUntilData

            binding.ivMicrophone.setVisible(it.isShow)
        }
    }
}