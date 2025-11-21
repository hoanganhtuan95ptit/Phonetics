package com.simple.phonetics.ui.game.items.ipa_puzzle

import android.content.ComponentCallbacks
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.lifecycle.asFlow
import com.google.android.flexbox.JustifyContent
import com.simple.adapter.MultiAdapter
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.ui.adapters.texts.ClickTextAdapter
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.doOnChangeHeightStatusAndHeightNavigation
import com.unknown.coroutines.launchCollect
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setText
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.ext.updateMargin
import com.simple.coreapp.utils.extentions.isActive
import com.simple.crashlytics.logCrashlytics
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Id
import com.simple.phonetics.R
import com.simple.phonetics.ui.game.GameFragment
import com.simple.phonetics.ui.game.items.GameItemFragment
import com.simple.phonetics.utils.exts.collectWithLockTransitionIfCached
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.exts.createFlexboxLayoutManager
import com.simple.phonetics.utils.exts.submitListAwaitV2
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.state.isSuccess
import java.util.UUID

class GameIPAPuzzleFragment : GameItemFragment<GameIPAPuzzleViewModel>() {

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
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        val clickTextAdapter = ClickTextAdapter { view, item ->

            if (item.id.startsWith(Id.CHOOSE)) {
                viewModel.updateChoose(item.data.asObjectOrNull<String>() ?: return@ClickTextAdapter)
            }
        }

        MultiAdapter(clickTextAdapter).apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null

            val layoutManager = createFlexboxLayoutManager(context = context) {

                logCrashlytics(
                    event = "GAME_IPA_PUZZLE",
                    throwable = it,
                    "VIEW_ITEM_SIZE" to "${viewModel.viewItemList.value?.size}"
                )
            }

            layoutManager.justifyContent = JustifyContent.FLEX_START
            binding.recyclerView.layoutManager = layoutManager
        }
    }

    private fun observeData() = with(viewModel) {

        val fragment = this@GameIPAPuzzleFragment

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


        actionInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "BUTTON") {

            val binding = binding?.frameConfirm ?: return@collectWithLockTransitionUntilData

            binding.btnConfirm.setText(it.text)

            binding.root.isClickable = it.isClickable
            binding.root.setBackground(it.background)

            binding.root.setVisible(true)
        }

        viewItemList.collectWithLockTransitionIfCached(fragment = fragment, tag = "VIEW_ITEM_LIST") { data, isFirst ->

            val binding = binding ?: return@collectWithLockTransitionIfCached

            binding.recyclerView.submitListAwaitV2(viewItemList = data, isFromCache = isFirst)
        }
    }
}

@Deeplink
class GameIPAPuzzleDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return DeeplinkManager.GAME_IPA_PUZZLE
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is GameFragment) return false

        val fragment = GameIPAPuzzleFragment()
        fragment.arguments = bundleOf(*extras?.toList().orEmpty().toTypedArray())

        val fragmentTransaction = componentCallbacks.childFragmentManager
            .beginTransaction()

        sharedElement?.forEach { (t, u) ->

            fragmentTransaction.addSharedElement(u, t)
        }

        if (isActive()) fragmentTransaction
            .replace(R.id.fragment_container, fragment, "")
            .commitAllowingStateLoss()

        return true
    }
}