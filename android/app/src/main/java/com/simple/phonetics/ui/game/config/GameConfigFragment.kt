package com.simple.phonetics.ui.game.config

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.os.bundleOf
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.JustifyContent
import com.simple.adapter.MultiAdapter
import com.simple.analytics.logAnalytics
import com.simple.core.utils.extentions.asObject
import com.simple.coreapp.ui.adapters.texts.ClickTextAdapter
import com.simple.coreapp.ui.base.dialogs.sheet.BaseViewModelSheetFragment
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.extentions.observeQueue
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.Deeplink
import com.simple.phonetics.EventName
import com.simple.phonetics.Id
import com.simple.phonetics.Param
import com.simple.phonetics.databinding.DialogListBinding
import com.simple.phonetics.entities.Word
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.fragments.BaseSheetFragment
import com.simple.phonetics.ui.game.GameConfigViewModel
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.phonetics.utils.exts.ListPreviewAdapter
import com.simple.phonetics.utils.exts.createFlexboxLayoutManager
import com.simple.phonetics.utils.sendEvent

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

        setupRecyclerView()

        observeData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
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

        adapter = MultiAdapter(clickTextAdapter, *ListPreviewAdapter()).apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null
            binding.recyclerView.setItemViewCacheSize(10)

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

            binding.root.delegate.backgroundColor = it.colorBackground
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
        return Deeplink.GAME_CONFIG
    }

    override suspend fun navigation(activity: ComponentActivity, deepLink: String, extras: Bundle?, sharedElement: Map<String, View>?): Boolean {

        if (activity !is MainActivity) return false

        val fragment = GameConfigFragment()
        fragment.arguments = extras
        fragment.showOrAwaitDismiss(activity.supportFragmentManager, tag = "")

        return true
    }
}