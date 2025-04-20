package com.simple.phonetics.ui.game.items.ipa_wordle

import android.content.ComponentCallbacks
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.lifecycle.asFlow
import com.google.android.flexbox.JustifyContent
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.ui.adapters.texts.ClickTextAdapter
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.doOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.ext.updateMargin
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Id
import com.simple.phonetics.R
import com.simple.phonetics.entities.Phonetic
import com.simple.phonetics.ui.base.adapters.ImageStateAdapter
import com.simple.phonetics.ui.game.GameFragment
import com.simple.phonetics.ui.game.items.GameItemFragment
import com.simple.phonetics.utils.exts.collectWithLockTransitionIfCached
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.exts.createFlexboxLayoutManager
import com.simple.phonetics.utils.exts.submitListAwaitV2
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.state.isCompleted
import com.simple.state.isRunning
import com.simple.state.isSuccess
import com.tuanha.adapter.MultiAdapter
import com.tuanha.deeplink.DeeplinkHandler
import com.tuanha.deeplink.sendDeeplink
import java.util.UUID

class GameIPAWordleFragment : GameItemFragment<GameIPAWordleViewModel>() {

    private var adapter by autoCleared<MultiAdapter>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.root.doOnChangeHeightStatusAndHeightNavigation(viewLifecycleOwner) { heightStatusBar: Int, heightNavigationBar: Int ->

            binding.recyclerView.updatePadding(left = DP.DP_8, right = DP.DP_8)
            binding.frameConfirm.root.updateMargin(bottom = heightNavigationBar + DP.DP_24)
        }

        binding.frameConfirm.root.setDebouncedClickListener {

            viewModel.checkChoose()
        }

        binding.frameHeader.root.setVisible(false)

        setupRecyclerView()

        observeData()
        observeConfigData()
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        val clickTextAdapter = ClickTextAdapter { view, item ->

            if (item.id.startsWith(Id.CHOOSE)) {
                viewModel.updateChoose(item.data.asObjectOrNull<Phonetic>() ?: return@ClickTextAdapter)
            }
        }

        val imageStateAdapter = ImageStateAdapter { view, item ->

            if (item.id == Id.LISTEN) {

                listen()
            }
        }

        MultiAdapter(clickTextAdapter, imageStateAdapter).apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null

            val layoutManager = createFlexboxLayoutManager(context = context) {

                logCrashlytics(
                    event = "GAME_IPA_WORDLE",
                    throwable = it,
                    "VIEW_ITEM_SIZE" to "${viewModel.viewItemList.value?.size}"
                )
            }

            layoutManager.justifyContent = JustifyContent.FLEX_START
            binding.recyclerView.layoutManager = layoutManager
        }
    }

    private fun observeData() = with(viewModel) {

        val fragment = this@GameIPAWordleFragment

        checkState.observe(viewLifecycleOwner) {

            it.doSuccess {
                gameViewModel.updateAnswerCorrect(true)
            }

            it.doFailed {
                gameViewModel.updateAnswerCorrect(false)
            }
        }

        stateInfoEvent.asFlow().launchCollect(viewLifecycleOwner) { event ->

            val info = event.getContentIfNotHandled() ?: return@launchCollect

            showPopupInfo(info = info, state = checkState.value ?: return@launchCollect)


            val binding = binding?.frameConfirm ?: return@launchCollect

            val transitionName = UUID.randomUUID().toString()
            binding.root.transitionName = transitionName

            if (checkState.value.isSuccess()) sendDeeplink(
                deepLink = gameConfigViewModel.getNextGame(),
                extras = mapOf(com.simple.phonetics.Param.ROOT_TRANSITION_NAME to transitionName),
                sharedElement = mapOf(transitionName to binding.root)
            ) else {

                viewModel.updateChoose(null)
            }
        }


        buttonInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "BUTTON") {

            val binding = binding?.frameConfirm ?: return@collectWithLockTransitionUntilData

            binding.btnConfirm.text = it.text

            binding.root.isClickable = it.isClickable
            binding.root.delegate.setBackground(it.background)

            binding.root.setVisible(true)
        }

        viewItemList.collectWithLockTransitionIfCached(fragment = fragment, tag = "VIEW_ITEM_LIST") { data, isFirst ->

            val binding = binding ?: return@collectWithLockTransitionIfCached

            binding.recyclerView.submitListAwaitV2(viewItemList = data, isFirst = isFirst)
        }
    }

    private fun observeConfigData() = with(configViewModel) {

        listenerEnable.observe(viewLifecycleOwner) {

            viewModel.updateListenerEnable(it)
        }
    }

    private fun listen() {

        val voiceState = viewModel.listenState.value

        if (voiceState.isRunning()) {

            viewModel.stopListen()
        } else if (voiceState == null || voiceState.isCompleted()) viewModel.startListen(
            voiceId = configViewModel.voiceSelect.value ?: 0,
            voiceSpeed = configViewModel.voiceSpeed.value ?: 1f
        )
    }
}

@com.tuanha.deeplink.annotation.Deeplink
class GameIPAWordleDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return DeeplinkManager.GAME_IPA_WORDLE
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is GameFragment) return false

        val fragment = GameIPAWordleFragment()
        fragment.arguments = bundleOf(*extras?.toList().orEmpty().toTypedArray())

        val fragmentTransaction = componentCallbacks.childFragmentManager
            .beginTransaction()

        sharedElement?.forEach { (t, u) ->

            fragmentTransaction.addSharedElement(u, t)
        }

        fragmentTransaction
            .replace(R.id.fragment_container, fragment, "")
            .commit()

        return true
    }
}