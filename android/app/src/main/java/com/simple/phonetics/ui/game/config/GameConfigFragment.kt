package com.simple.phonetics.ui.game.config

import android.content.ComponentCallbacks
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.JustifyContent
import com.simple.adapter.MultiAdapter
import com.simple.analytics.logAnalytics
import com.simple.core.utils.extentions.asObject
import com.simple.coreapp.ui.adapters.texts.ClickTextAdapter
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.doOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.extentions.observeQueue
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.EventName
import com.simple.phonetics.Id
import com.simple.phonetics.Param
import com.simple.phonetics.databinding.DialogListBinding
import com.simple.phonetics.entities.Word
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.fragments.BaseSheetFragment
import com.simple.phonetics.ui.game.GameConfigViewModel
import com.simple.phonetics.utils.exts.createFlexboxLayoutManager
import com.simple.phonetics.utils.sendEvent
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink

class GameConfigFragment : BaseSheetFragment<DialogListBinding, GameConfigViewModel>() {


    override val viewModel: GameConfigViewModel by lazy {
        getViewModel(requireActivity(), GameConfigViewModel::class)
    }


    private var result: Int = -1

    private var adapter by autoCleared<MultiAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logAnalytics("game_config_show")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.root.doOnChangeHeightStatusAndHeightNavigation(viewLifecycleOwner) { heightStatusBar: Int, heightNavigationBar: Int ->

            binding.recyclerView.updatePadding(bottom = heightNavigationBar + DP.DP_24)
        }

        setupRecyclerView()

        observeData()
    }

    override fun onDestroy() {
        super.onDestroy()
        sendEvent(EventName.DISMISS, bundleOf(Param.RESULT to result))
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        val clickTextAdapter = ClickTextAdapter { view, item ->

            if (item.id.startsWith(Id.RESOURCE)) {

                viewModel.updateResource(item.data.asObject<Word.Resource>())
            } else if (item.id.startsWith(Id.BUTTON)) {

                result = 1
                dismiss()
            }
        }

        MultiAdapter(clickTextAdapter).apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null

            val layoutManager = createFlexboxLayoutManager(context = context) {

                logCrashlytics(
                    event = "GAME_CONFIG",
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

@Deeplink
class ConfigDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return DeeplinkManager.GAME_CONFIG
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is MainActivity) return false

        val fragment = GameConfigFragment()
        fragment.arguments = bundleOf(*extras?.toList().orEmpty().toTypedArray())
        fragment.showOrAwaitDismiss(componentCallbacks.supportFragmentManager, tag = "")

        return true
    }
}