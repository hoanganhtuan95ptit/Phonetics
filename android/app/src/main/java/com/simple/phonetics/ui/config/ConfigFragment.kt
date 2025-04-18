package com.simple.phonetics.ui.config

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.JustifyContent
import com.simple.adapter.MultiAdapter
import com.simple.core.utils.extentions.asObject
import com.simple.coreapp.ui.adapters.texts.ClickTextAdapter
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.extentions.observeQueue
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.Deeplink
import com.simple.phonetics.Id
import com.simple.phonetics.databinding.DialogListBinding
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.fragments.BaseSheetFragment
import com.simple.phonetics.ui.config.adapters.VoiceSpeedAdapter
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.phonetics.utils.exts.ListPreviewAdapter
import com.simple.phonetics.utils.exts.createFlexboxLayoutManager

class ConfigFragment : BaseSheetFragment<DialogListBinding, ConfigViewModel>() {

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

        val clickTextAdapter = ClickTextAdapter { view, item ->

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

        adapter = MultiAdapter(clickTextAdapter, voiceSpeedAdapter, *ListPreviewAdapter()).apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null

            val layoutManager = createFlexboxLayoutManager(context = context) {

                logCrashlytics(
                    event = "CONFIG",
                    throwable = it,
                    "VIEW_ITEM_SIZE" to "${viewModel.viewItemList.value?.size}"
                )
            }
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.FLEX_START
            binding.recyclerView.layoutManager = layoutManager
        }
    }

    private fun observeData() = with(viewModel) {

        theme.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.root.delegate.setBackground(Background(backgroundColor = it.colorBackground, cornerRadius_TL = DP.DP_16, cornerRadius_TR = DP.DP_16))
            binding.vAnchor.delegate.setBackground(Background(backgroundColor = it.colorDivider, cornerRadius = DP.DP_100))
        }

        viewItemList.observeQueue(viewLifecycleOwner) {

            val binding = binding ?: return@observeQueue

            binding.recyclerView.submitListAwait(it)
        }
    }
}

@com.tuanha.deeplink.annotation.Deeplink
class ConfigDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return Deeplink.CONFIG
    }

    override suspend fun navigation(activity: ComponentActivity, deepLink: String, extras: Bundle?, sharedElement: Map<String, View>?): Boolean {

        if (activity !is MainActivity) return false

        val fragment = ConfigFragment()
        fragment.arguments = extras
        fragment.showOrAwaitDismiss(activity.supportFragmentManager, tag = "")

        return true
    }
}