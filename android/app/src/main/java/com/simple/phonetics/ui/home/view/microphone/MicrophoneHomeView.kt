package com.simple.phonetics.ui.home.view.microphone

import com.google.auto.service.AutoService
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.EventName
import com.simple.phonetics.Param
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.view.HomeView
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.showAds
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

@AutoService(HomeView::class)
class MicrophoneHomeView : HomeView {

    override fun setup(fragment: HomeFragment) {

        val viewModel: HomeViewModel by fragment.viewModel()

        val configViewModel: ConfigViewModel by fragment.activityViewModel()

        val microphoneHomeViewModel: MicrophoneHomeViewModel by fragment.viewModel()


        fragment.binding?.ivMicrophone?.setDebouncedClickListener {

            sendDeeplink(DeeplinkManager.RECORDING, extras = mapOf(Param.REVERSE to viewModel.isReverse.value, Param.KEY_REQUEST to EventName.MICROPHONE))
        }

        com.simple.event.listenerEvent(lifecycle = fragment.viewLifecycleOwner.lifecycle, eventName = EventName.MICROPHONE) {

            val result = it.asObjectOrNull<String>() ?: return@listenerEvent
            val binding = fragment.binding ?: return@listenerEvent

            viewModel.getPhonetics("")
            binding.etText.setText(result)

            showAds()
        }

        observeData(fragment = fragment, viewModel = viewModel, configViewModel = configViewModel, microphoneHomeViewModel = microphoneHomeViewModel)
        observeMicrophoneData(fragment = fragment, viewModel = viewModel, configViewModel = configViewModel, microphoneHomeViewModel = microphoneHomeViewModel)
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