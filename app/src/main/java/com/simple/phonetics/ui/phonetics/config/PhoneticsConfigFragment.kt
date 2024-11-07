package com.simple.phonetics.ui.phonetics.config

import android.os.Bundle
import android.view.View
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.simple.adapter.MultiAdapter
import com.simple.coreapp.ui.base.dialogs.sheet.BaseViewModelSheetFragment
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.extentions.observeQueue
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.phonetics.databinding.FragmentConfigBinding
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.adapters.TitleAdapter
import com.simple.phonetics.ui.phonetics.config.adapters.PhoneticCodeAdapter
import com.simple.phonetics.ui.phonetics.config.adapters.TranslationAdapter
import com.simple.phonetics.ui.phonetics.config.adapters.VoiceCodeAdapter
import com.simple.phonetics.ui.phonetics.config.adapters.VoiceSpeedAdapter

class PhoneticsConfigFragment : BaseViewModelSheetFragment<FragmentConfigBinding, ConfigViewModel>() {

    override val viewModel: ConfigViewModel by lazy {
        getViewModel(requireActivity(), ConfigViewModel::class)
    }

    private var adapter by autoCleared<MultiAdapter>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        observeData()
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        val voiceCodeAdapter = VoiceCodeAdapter { _, item ->

            viewModel.updateVoiceSelect(item.data)
        }

        val voiceSpeedAdapter = VoiceSpeedAdapter { _, item ->

            viewModel.updateVoiceSpeed(item.current)
        }

        val translationAdapter = TranslationAdapter { _, item ->

            if (item.id.isEmpty()) return@TranslationAdapter

            viewModel.updateTranslation(item.id)
        }

        val phoneticCodeAdapter = PhoneticCodeAdapter { _, item ->

            viewModel.updatePhoneticSelect(item.data)
        }

        adapter = MultiAdapter(TitleAdapter(), voiceCodeAdapter, voiceSpeedAdapter, translationAdapter, phoneticCodeAdapter).apply {

            val layoutManager = FlexboxLayoutManager(context)
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.FLEX_START

            binding.recyclerView.adapter = this
            binding.recyclerView.layoutManager = layoutManager
        }
    }

    private fun observeData() = with(viewModel) {

        listViewItem.observeQueue(viewLifecycleOwner) {

            val binding = binding ?: return@observeQueue

            binding.recyclerView.submitListAwait(it)
        }
    }
}