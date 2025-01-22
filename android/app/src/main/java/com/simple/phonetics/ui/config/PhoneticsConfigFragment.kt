package com.simple.phonetics.ui.config

import android.os.Bundle
import android.view.View
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.simple.adapter.MultiAdapter
import com.simple.core.utils.extentions.asObject
import com.simple.coreapp.ui.adapters.TextAdapter
import com.simple.coreapp.ui.base.dialogs.sheet.BaseViewModelSheetFragment
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.extentions.observeQueue
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.phonetics.Id
import com.simple.phonetics.databinding.DialogConfigBinding
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.config.adapters.VoiceSpeedAdapter

class PhoneticsConfigFragment : BaseViewModelSheetFragment<DialogConfigBinding, ConfigViewModel>() {

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

        val textAdapter = TextAdapter { view, item ->

            if (item.id.startsWith(Id.TRANSLATE)) {

                viewModel.updateTranslation(item.data.asObject<Pair<String, Boolean>>().first)
            } else if (item.id.startsWith(Id.IPA)) {

                viewModel.updatePhoneticSelect(item.data.asObject<Pair<String, Boolean>>().first)
            } else if (item.id.startsWith(Id.VOICE)) {

                viewModel.updateVoiceSelect(item.data.asObject<Pair<Int, Boolean>>().first)
            }
        }

        val voiceSpeedAdapter = VoiceSpeedAdapter { _, item ->

            viewModel.updateVoiceSpeed(item.current)
        }

        adapter = MultiAdapter(textAdapter, voiceSpeedAdapter).apply {

            val layoutManager = FlexboxLayoutManager(context)
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.FLEX_START

            binding.recyclerView.adapter = this
            binding.recyclerView.layoutManager = layoutManager
        }
    }

    private fun observeData() = with(viewModel) {

        theme.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.root.delegate.backgroundColor = it.colorBackground
        }

        viewItemList.observeQueue(viewLifecycleOwner) {

            val binding = binding ?: return@observeQueue

            binding.recyclerView.submitListAwait(it)
        }
    }
}