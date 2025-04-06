package com.simple.phonetics.ui.game.items

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.extentions.get
import com.simple.phonetics.Deeplink
import com.simple.phonetics.EventName
import com.simple.phonetics.Param
import com.simple.phonetics.R
import com.simple.phonetics.databinding.FragmentListHeaderHorizontalBinding
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.base.fragments.BaseFragment
import com.simple.phonetics.ui.game.GameConfigViewModel
import com.simple.phonetics.ui.game.GameViewModel
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.exts.playMedia
import com.simple.phonetics.utils.exts.playVibrate
import com.simple.phonetics.utils.listenerEvent
import com.simple.phonetics.utils.sendDeeplink
import com.simple.phonetics.utils.showAds
import com.simple.state.ResultState
import com.simple.state.isCompleted
import com.simple.state.isFailed
import com.simple.state.isSuccess
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

abstract class GameItemFragment<VM : GameItemViewModel> : BaseFragment<FragmentListHeaderHorizontalBinding, VM>() {

    protected val gameViewModel: GameViewModel by lazy {
        getViewModel(requireParentFragment(), GameViewModel::class)
    }

    protected val configViewModel: ConfigViewModel by lazy {
        getViewModel(requireActivity(), ConfigViewModel::class)
    }

    protected val gameConfigViewModel: GameConfigViewModel by lazy {
        getViewModel(requireActivity(), GameConfigViewModel::class)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeData()
        observeConfigData()

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

            binding.root.setBackgroundColor(it.colorBackground)
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

    protected suspend fun showPopupInfo(info: GameItemViewModel.StateInfo, state: ResultState<String>) = channelFlow {

        if (state.isFailed()) launch {
            playVibrate()
        }

        if (state.isCompleted()) launch {
            playMedia(if (state.isSuccess()) R.raw.mp3_answer_correct else R.raw.mp3_answer_failed)
        }


        listenerEvent(coroutineScope = this, EventName.DISMISS) {

            trySend(Unit)
        }


        val consecutiveCorrectAnswers = gameViewModel.consecutiveCorrectAnswer.get()

        val extras = if (consecutiveCorrectAnswers.first > 0 && consecutiveCorrectAnswers.second) bundleOf(

            Param.NUMBER to consecutiveCorrectAnswers.first
        ) else bundleOf(

            com.simple.coreapp.Param.CANCEL to false,

            com.simple.coreapp.Param.ANIM to info.anim,

            com.simple.coreapp.Param.TITLE to info.title,
            com.simple.coreapp.Param.MESSAGE to info.message,

            com.simple.coreapp.Param.BACKGROUND to info.background,

            com.simple.coreapp.Param.POSITIVE to info.positive,
        )

        if (consecutiveCorrectAnswers.first > 0 && consecutiveCorrectAnswers.second) {
            sendDeeplink(Deeplink.GAME_CONGRATULATION, extras = extras)
        } else {
            sendDeeplink(Deeplink.CONFIRM, extras = extras)
        }
    }.first()
}