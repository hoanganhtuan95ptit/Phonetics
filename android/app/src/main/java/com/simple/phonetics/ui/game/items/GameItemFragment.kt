package com.simple.phonetics.ui.game.items

import android.media.MediaPlayer
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
import com.simple.phonetics.utils.listenerEvent
import com.simple.phonetics.utils.sendDeeplink
import com.simple.state.ResultState
import com.simple.state.isSuccess
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first

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
    }

    private fun observeData() = with(viewModel) {

        val fragment = this@GameItemFragment

        theme.collectWithLockTransitionUntilData(fragment = fragment, tag = "THEME") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.root.setBackgroundColor(it.colorBackground)
        }
    }

    protected suspend fun showPopupInfo(info: GameItemViewModel.StateInfo, state: ResultState<String>) = channelFlow {

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

        awaitClose {

            mediaPlayer.release()
        }
    }.first()
}