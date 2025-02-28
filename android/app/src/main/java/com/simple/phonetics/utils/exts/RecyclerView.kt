package com.simple.phonetics.utils.exts

import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.base.fragments.transition.TransitionFragment
import com.simple.coreapp.utils.extentions.submitListAwait
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

suspend fun RecyclerView.submitListAwait(
    transitionFragment: TransitionFragment<*, *>,
    viewItemList: List<ViewItem>,
    isFirst: Boolean = false,
    tag: String = ""
) {

    val isAnim = !isFirst

    if (isFirst) {
        transitionFragment.lockTransition(tag = tag)
    }

    if (isAnim) {
        transitionFragment.viewModel.awaitTransition()
    }

    submitListAwait(viewItemList = viewItemList)

    if (isAnim) {
        transitionAwait()
    }

    if (isFirst) {
        transitionFragment.unlockTransition(tag = tag)
    }
}

suspend fun RecyclerView.transitionAwait(transition: Transition = TransitionSet().addTransition(ChangeBounds().setDuration(350)).addTransition(Fade().setDuration(350))) = channelFlow<Unit> {

    val timeoutJob = launch {

        delay(350)
        trySend(Unit)
    }

    val transitionListener = object : Transition.TransitionListener {

        override fun onTransitionStart(transition: Transition) {
            timeoutJob.cancel()
        }

        override fun onTransitionEnd(transition: Transition) {
            trySend(Unit)
        }

        override fun onTransitionCancel(transition: Transition) {
            trySend(Unit)
        }

        override fun onTransitionPause(transition: Transition) {
        }

        override fun onTransitionResume(transition: Transition) {
        }
    }

    TransitionManager.beginDelayedTransition(this@transitionAwait, transition.addListener(transitionListener))

    awaitClose {
        transition.removeListener(transitionListener)
        TransitionManager.endTransitions(this@transitionAwait)
    }
}.first()