package com.simple.phonetics.ui.game.items

import android.os.Bundle
import android.view.View
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.extentions.get
import com.simple.deeplink.sendDeeplink
import com.simple.event.listenerEvent
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.R
import com.simple.phonetics.databinding.FragmentListHeaderHorizontalBinding
import com.simple.phonetics.ui.base.fragments.BaseFragment
import com.simple.phonetics.ui.game.GameConfigViewModel
import com.simple.phonetics.ui.game.GameViewModel
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.exts.getOrTransparent
import com.simple.phonetics.utils.exts.playMedia
import com.simple.phonetics.utils.exts.playVibrate
import com.simple.phonetics.utils.showAds
import com.simple.state.ResultState
import com.simple.state.isCompleted
import com.simple.state.isFailed
import com.simple.state.isSuccess
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

abstract class GameItemFragment<VM : GameItemViewModel> : BaseFragment<FragmentListHeaderHorizontalBinding, VM>() {

    protected val gameViewModel: GameViewModel by lazy {
        getViewModel(requireParentFragment(), GameViewModel::class)
    }

    protected val gameConfigViewModel: GameConfigViewModel by lazy {
        getViewModel(requireActivity(), GameConfigViewModel::class)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeData()

        observeGameData()
        observeGameConfigData()

        if (arguments?.getString(Param.ROOT_TRANSITION_NAME, "").orEmpty().isNotEmpty()) {
            showAds()
        }
    }

    private fun observeData() = with(viewModel) {

        val fragment = this@GameItemFragment

        theme.collectWithLockTransitionUntilData(fragment = fragment, tag = "THEME") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.root.setBackgroundColor(it.getOrTransparent("colorBackground"))
        }
    }

    private fun observeGameData() = with(gameViewModel) {

        text.observe(viewLifecycleOwner) {

            viewModel.updateText(it)
        }

        consecutiveCorrectAnswerEvent.observe(viewLifecycleOwner) {

            viewModel.updateConsecutiveCorrectAnswer(it)
        }
    }

    private fun observeGameConfigData() = with(gameConfigViewModel) {

        resourceSelected.observe(viewLifecycleOwner) {

            viewModel.updateResourceSelected(it)
        }
    }

    protected suspend fun showPopupInfo(info: GameItemViewModel.StateInfo, state: ResultState<String>) = channelFlow {

        if (state.isFailed()) launch {
            playVibrate()
        }

        if (state.isCompleted()) launch {
            playMedia(if (state.isSuccess()) R.raw.mp3_answer_correct else R.raw.mp3_answer_failed)
        }


        val keyRequest = "GAME_STATUS"

        listenerEvent(coroutineScope = this, keyRequest) {

            trySend(Unit)
        }


        val consecutiveCorrectAnswers = gameViewModel.consecutiveCorrectAnswer.get()

        if (consecutiveCorrectAnswers.first > 0 && consecutiveCorrectAnswers.second) {
            showGameCongratulation(number = consecutiveCorrectAnswers.first, keyRequest = keyRequest)
        } else {
            showMessage(info = info, keyRequest = keyRequest)
        }

        awaitClose {
        }
    }.first()

    private fun showMessage(info: GameItemViewModel.StateInfo, keyRequest: String) {

        val background = Background(
            cornerRadius_TL = DP.DP_24,
            cornerRadius_TR = DP.DP_24,
            backgroundColor = info.backgroundColor
        )

        val extras = mapOf(

            com.simple.coreapp.Param.CANCEL to false,
            com.simple.coreapp.Param.POSITIVE to info.positive,
            com.simple.coreapp.Param.BACKGROUND to background,

            Param.KEY_REQUEST to keyRequest,
            Param.VIEW_ITEM_LIST to info.viewItemList
        )

        sendDeeplink(DeeplinkManager.CONFIRM, extras = extras)
    }

    private fun showGameCongratulation(number: Long, keyRequest: String) {

        val extras = mapOf(

            Param.NUMBER to number,
            Param.KEY_REQUEST to keyRequest
        )

        sendDeeplink(DeeplinkManager.GAME_CONGRATULATION, extras = extras)
    }
}