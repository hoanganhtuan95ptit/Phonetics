package com.simple.phonetics.ui.game.items.ipa_match

import android.content.ComponentCallbacks
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.lifecycle.asFlow
import com.google.android.flexbox.JustifyContent
import com.simple.adapter.MultiAdapter
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.Param
import com.simple.coreapp.ui.adapters.texts.ClickTextAdapter
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.doOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.ext.updateMargin
import com.simple.coreapp.utils.extentions.get
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.Deeplink
import com.simple.phonetics.EventName
import com.simple.phonetics.R
import com.simple.phonetics.databinding.FragmentListHeaderHorizontalBinding
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.base.adapters.ImageStateAdapter
import com.simple.phonetics.ui.base.fragments.BaseFragment
import com.simple.phonetics.ui.game.GameConfigViewModel
import com.simple.phonetics.ui.game.GameFragment
import com.simple.phonetics.ui.game.GameViewModel
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.phonetics.utils.exts.ListPreviewAdapter
import com.simple.phonetics.utils.exts.collectWithLockTransitionIfCached
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.exts.createFlexboxLayoutManager
import com.simple.phonetics.utils.exts.submitListAwaitV2
import com.simple.phonetics.utils.listenerEvent
import com.simple.phonetics.utils.sendDeeplink
import com.simple.state.ResultState
import com.simple.state.doFailed
import com.simple.state.doSuccess
import com.simple.state.isCompleted
import com.simple.state.isFailed
import com.simple.state.isRunning
import com.simple.state.isSuccess
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import java.util.UUID

class GameIPAMatchFragment : BaseFragment<FragmentListHeaderHorizontalBinding, GameIPAMatchViewModel>() {

    private val gameViewModel: GameViewModel by lazy {
        getViewModel(requireParentFragment(), GameViewModel::class)
    }

    private val configViewModel: ConfigViewModel by lazy {
        getViewModel(requireActivity(), ConfigViewModel::class)
    }

    private val gameConfigViewModel: GameConfigViewModel by lazy {
        getViewModel(requireActivity(), GameConfigViewModel::class)
    }

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

        observeGameData()
        observeGameConfigData()
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        val clickTextAdapter = ClickTextAdapter { view, item ->

            val data = item.data.asObjectOrNull<Pair<GameIPAMatchViewModel.Option?, GameIPAMatchViewModel.Type?>>() ?: return@ClickTextAdapter

            viewModel.updateChoose(data)
        }

        val imageStateAdapter = ImageStateAdapter { view, item ->

            val data = item.data.asObjectOrNull<Pair<GameIPAMatchViewModel.Option?, GameIPAMatchViewModel.Type?>>() ?: return@ImageStateAdapter

            viewModel.updateChoose(data)

            listen(data = data)
        }

        adapter = MultiAdapter(clickTextAdapter, imageStateAdapter, *ListPreviewAdapter()).apply {

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

        val fragment = this@GameIPAMatchFragment

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
            val state = checkState.value ?: return@launchCollect

            showPopupInfo(info = info, state = state)

            if (state.isFailed()) {

                viewModel.updateWaring(true)
                delay(2000)
                viewModel.updateWaring(false)
            }

            val binding = binding?.frameConfirm ?: return@launchCollect

            val transitionName = UUID.randomUUID().toString()
            binding.root.transitionName = transitionName

            if (state.isSuccess()) sendDeeplink(
                deepLink = gameConfigViewModel.getNextGame(),
                extras = bundleOf(com.simple.phonetics.Param.ROOT_TRANSITION_NAME to transitionName),
                sharedElement = mapOf(transitionName to binding.root)
            ) else {
                viewModel.resetChoose()
            }
        }


        theme.collectWithLockTransitionUntilData(fragment = fragment, tag = "THEME") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.root.setBackgroundColor(it.colorBackground)
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

    private fun observeGameData() = with(gameViewModel) {

        consecutiveCorrectAnswerEvent.observe(viewLifecycleOwner) {

            viewModel.updateConsecutiveCorrectAnswer(it)
        }
    }

    private fun observeGameConfigData() = with(gameConfigViewModel) {

        resourceSelected.observe(viewLifecycleOwner) {

            viewModel.updateResourceSelected(it)
        }
    }

    private fun listen(data: Pair<GameIPAMatchViewModel.Option?, GameIPAMatchViewModel.Type?>) {

        viewModel.startListen(
            data = Pair(data.first ?: return, data.second ?: return),
            voiceId = configViewModel.voiceSelect.value ?: 0,
            voiceSpeed = configViewModel.voiceSpeed.value ?: 1f
        )
    }

    private suspend fun showPopupInfo(info: GameIPAMatchViewModel.StateInfo, state: ResultState<String>) = channelFlow {

        val source = if (state.isSuccess()) {
            R.raw.mp3_answer_correct
        } else {
            R.raw.mp3_answer_failed
        }

        val mediaPlayer = MediaPlayer.create(context, source)
        mediaPlayer.start()

        listenerEvent(coroutineScope = this, EventName.DISMISS) {

            trySend(Unit)
        }


        val consecutiveCorrectAnswers = gameViewModel.consecutiveCorrectAnswer.get()

        val extras = if (consecutiveCorrectAnswers.first > 0 && consecutiveCorrectAnswers.second) bundleOf(

            com.simple.phonetics.Param.NUMBER to consecutiveCorrectAnswers.first
        ) else bundleOf(

            Param.CANCEL to false,

            Param.ANIM to info.anim,

            Param.TITLE to info.title,
            Param.MESSAGE to info.message,

            Param.BACKGROUND to info.background,

            Param.POSITIVE to info.positive,
        )

        if (consecutiveCorrectAnswers.first > 0 && consecutiveCorrectAnswers.second) {
            sendDeeplink(Deeplink.GAME_CONGRATULATION, extras = extras)
        } else {
            sendDeeplink(Deeplink.CONFIRM, extras = extras)
        }

        awaitClose {

            mediaPlayer.release()
        }
    }.first()
}

@com.tuanha.deeplink.annotation.Deeplink
class GameIPAMatchDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return Deeplink.GAME_IPA_MATCH
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Bundle?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is GameFragment) return false

        val fragment = GameIPAMatchFragment()
        fragment.arguments = extras

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