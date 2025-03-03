package com.simple.phonetics.ui.phonetics.view.microphone

import androidx.core.os.bundleOf
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.phonetics.Deeplink
import com.simple.phonetics.EventName
import com.simple.phonetics.Param
import com.simple.phonetics.ui.phonetics.PhoneticsFragment
import com.simple.phonetics.ui.phonetics.PhoneticsViewModel
import com.simple.phonetics.utils.listenerEvent
import com.simple.phonetics.utils.sendDeeplink
import org.koin.androidx.viewmodel.ext.android.viewModel

interface MicrophoneView {

    fun setupMicrophone(fragment: PhoneticsFragment)
}

class MicrophoneViewImpl : MicrophoneView {

    override fun setupMicrophone(fragment: PhoneticsFragment) {

        val viewModel: PhoneticsViewModel by fragment.viewModel()

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

        microphoneViewModel.microphoneInfo.observe(fragment.viewLifecycleOwner) {

            val binding = fragment.binding ?: return@observe

            binding.ivMicrophone.setVisible(it.isShow)
        }
    }
}