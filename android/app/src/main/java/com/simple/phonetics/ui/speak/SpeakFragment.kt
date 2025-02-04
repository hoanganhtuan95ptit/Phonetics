package com.simple.phonetics.ui.speak

import android.os.Bundle
import android.view.View
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.simple.adapter.MultiAdapter
import com.simple.coreapp.ui.adapters.texts.ClickTextAdapter
import com.simple.coreapp.ui.adapters.texts.NoneTextAdapter
import com.simple.coreapp.ui.base.dialogs.sheet.BaseViewModelSheetFragment
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.extentions.observeQueue
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.phonetics.Param
import com.simple.phonetics.databinding.DialogListBinding
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.config.adapters.VoiceSpeedAdapter
import com.simple.phonetics.ui.phonetics.adapters.PhoneticsAdapter
import com.simple.phonetics.ui.speak.adapters.ImageStateAdapter
import com.simple.state.isCompleted
import com.simple.state.isRunning
import com.simple.state.toSuccess

class SpeakFragment : BaseViewModelSheetFragment<DialogListBinding, SpeakViewModel>() {

    private val configViewModel: ConfigViewModel by lazy {
        getViewModel(requireActivity(), ConfigViewModel::class)
    }

    private var adapter by autoCleared<MultiAdapter>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        observeData()
        observePhoneticsConfigData()
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        val clickTextAdapter = ClickTextAdapter { view, item ->

        }

        val phoneticsAdapter = PhoneticsAdapter { _, item ->

        }

        val voiceSpeedAdapter = VoiceSpeedAdapter { _, item ->

        }

        val imageStateAdapter = ImageStateAdapter { view, item ->

            val voiceState = viewModel.listenState.value

            if (item.id == SpeakViewModel.ID.LISTEN) if (voiceState.isRunning()) {

                viewModel.stopListen()
            } else if (voiceState == null || voiceState.isCompleted()) viewModel.startListen(

                voiceId = configViewModel.voiceSelect.value ?: 0,
                voiceSpeed = configViewModel.voiceSpeed.value ?: 1f
            )


            val speakState = viewModel.speakState.value

            if (item.id == SpeakViewModel.ID.SPEAK) if (speakState.isRunning()) {

                viewModel.stopSpeak()
            } else if (speakState == null || speakState.isCompleted()) {

                viewModel.startSpeak()
            }


        }

        adapter = MultiAdapter(clickTextAdapter, phoneticsAdapter, voiceSpeedAdapter, imageStateAdapter, NoneTextAdapter()).apply {

            val layoutManager = FlexboxLayoutManager(context)
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.CENTER

            binding.recyclerView.adapter = this
            binding.recyclerView.layoutManager = layoutManager
        }
    }

    private fun observeData() = with(viewModel) {

        theme.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.root.delegate.backgroundColor = it.colorBackground
            binding.root.delegate.setBgSelector()
        }

        viewItemList.observeQueue(viewLifecycleOwner) {

            val binding = binding ?: return@observeQueue

            binding.recyclerView.submitListAwait(it)
        }

        arguments?.getString(Param.TEXT)?.takeIf {

            it.isNotBlank()
        }?.let {

            this.updateText(it)
        }
    }

    private fun observePhoneticsConfigData() = with(configViewModel) {

        voiceState.observe(viewLifecycleOwner) {

            viewModel.updateSupportSpeak(it.toSuccess()?.data.orEmpty().isNotEmpty())
        }

        phoneticSelect.observe(viewLifecycleOwner) {

            viewModel.updatePhoneticSelect(it)
        }
    }
}