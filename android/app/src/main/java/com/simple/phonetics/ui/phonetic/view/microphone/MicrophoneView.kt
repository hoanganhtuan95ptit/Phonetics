package com.simple.phonetics.ui.phonetic.view.microphone

import androidx.core.os.bundleOf
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.phonetics.Deeplink
import com.simple.phonetics.EventName
import com.simple.phonetics.Param
import com.simple.phonetics.ui.phonetic.PhoneticsFragment
import com.simple.phonetics.ui.phonetic.PhoneticViewModel
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.listenerEvent
import com.simple.phonetics.utils.sendDeeplink
import org.koin.androidx.viewmodel.ext.android.viewModel

interface MicrophoneView {

    fun setupMicrophone(fragment: PhoneticsFragment)
}

class MicrophoneViewImpl : MicrophoneView {

    override fun setupMicrophone(fragment: PhoneticsFragment) {

        val viewModel: PhoneticViewModel by fragment.viewModel()

        val microphoneViewModel: MicrophoneViewModel by fragment.viewModel()


        fragment.binding?.ivMicrophone?.setDebouncedClickListener {

            sendDeeplink(Deeplink.RECORDING, extras = bundleOf(Param.REVERSE to viewModel.isReverse.value, Param.KEY_REQUEST to EventName.MICROPHONE))
        }


        listenerEvent(lifecycle = fragment.viewLifecycleOwner.lifecycle, eventName = EventName.MICROPHONE) {

            fragment.binding?.etText?.setText(it.asObjectOrNull<String>() ?: return@listenerEvent)
        }


        viewModel.isReverse.observe(fragment.viewLifecycleOwner) {

            microphoneViewModel.updateReverse(it)
        }

        microphoneViewModel.microphoneInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "MICROPHONE") {

            val binding = fragment.binding ?: return@collectWithLockTransitionUntilData

            binding.ivMicrophone.setVisible(it.isShow)
        }
    }
}