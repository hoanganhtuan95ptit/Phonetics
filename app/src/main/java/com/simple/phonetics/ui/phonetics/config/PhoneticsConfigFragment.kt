package com.simple.phonetics.ui.phonetics.config

import android.content.Context
import android.os.Bundle
import android.view.View
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.simple.adapter.MultiAdapter
import com.simple.analytics.logAnalytics
import com.simple.coreapp.ui.base.fragments.BaseViewModelSheetFragment
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.extentions.getViewModel
import com.simple.coreapp.utils.extentions.observeQueue
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.phonetics.databinding.FragmentConfigBinding
import com.simple.phonetics.ui.phonetics.config.adapters.PhoneticCodeAdapter
import com.simple.phonetics.ui.adapters.TitleAdapter
import com.simple.phonetics.ui.phonetics.config.adapters.TranslationAdapter
import com.simple.phonetics.ui.phonetics.config.adapters.VoiceCodeAdapter
import com.simple.phonetics.ui.phonetics.config.adapters.VoiceSpeedAdapter
import com.simple.bottomsheet.CustomBottomSheetDialog

class PhoneticsConfigFragment : BaseViewModelSheetFragment<FragmentConfigBinding, PhoneticsConfigViewModel>() {


    override val viewModel: PhoneticsConfigViewModel by lazy {
        getViewModel(requireParentFragment(), PhoneticsConfigViewModel::class)
    }


    private var adapter by autoCleared<MultiAdapter>()


    override fun onAttach(context: Context) {
        super.onAttach(context)

        logAnalytics("PHONETICS_CONFIG_FRAGMENT_OPEN" to "OPEN")
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (dialog as? CustomBottomSheetDialog)?.postponeEnterTransition()

        setupRecyclerView()

        observeData()
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        val voiceCodeAdapter = VoiceCodeAdapter { view, item ->

            logAnalytics("VOICE_CODE" to item.id)
            viewModel.updateVoiceSelect(item.id)
        }

        val voiceSpeedAdapter = VoiceSpeedAdapter { view, item ->

            logAnalytics("VOICE_SPEED" to item.current.toString())
            viewModel.updateVoiceSpeed(item.current)
        }

        val translationAdapter = TranslationAdapter { view, item ->

            if (item.id.isEmpty()) return@TranslationAdapter

            logAnalytics("TRANSLATION" to "${if (viewModel.translateSelect.value.isNullOrBlank()) id else ""}")
            viewModel.updateTranslation(item.id)
        }

        val phoneticCodeAdapter = PhoneticCodeAdapter { view, item ->

            logAnalytics("PHONETIC_CODE" to item.id)
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

            (dialog as? CustomBottomSheetDialog)?.startPostponedEnterTransition()
        }
    }
}