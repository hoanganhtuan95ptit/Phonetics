package com.simple.phonetics.ui.home.view.microphone

import androidx.core.os.bundleOf
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.phonetics.Deeplink
import com.simple.phonetics.EventName
import com.simple.phonetics.Param
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.listenerEvent
import com.simple.phonetics.utils.sendDeeplink
import org.koin.androidx.viewmodel.ext.android.viewModel

interface MicrophoneHomeView {

    fun setupMicrophone(fragment: HomeFragment)
}

class MicrophoneHomeViewImpl : MicrophoneHomeView {

    override fun setupMicrophone(fragment: HomeFragment) {

        val viewModel: HomeViewModel by fragment.viewModel()

        val microphoneHomeViewModel: MicrophoneHomeViewModel by fragment.viewModel()


        fragment.binding?.ivMicrophone?.setDebouncedClickListener {

            sendDeeplink(Deeplink.RECORDING, extras = bundleOf(Param.REVERSE to viewModel.isReverse.value, Param.KEY_REQUEST to EventName.MICROPHONE))
        }


        listenerEvent(lifecycle = fragment.viewLifecycleOwner.lifecycle, eventName = EventName.MICROPHONE) {

            val result = it.asObjectOrNull<String>() ?: return@listenerEvent
            val binding = fragment.binding ?: return@listenerEvent

            viewModel.getPhonetics("")
            binding.etText.setText(result)
        }


        viewModel.isReverse.observe(fragment.viewLifecycleOwner) {

            microphoneHomeViewModel.updateReverse(it)
        }

        microphoneHomeViewModel.microphoneInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "MICROPHONE") {

            val binding = fragment.binding ?: return@collectWithLockTransitionUntilData

            binding.ivMicrophone.setVisible(it.isShow)
        }
    }
}