package com.simple.phonetics.utils.exts

import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.Scene
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.extentions.submitListAwait
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

suspend fun RecyclerView.submitListAwaitV2(
    viewItemList: List<ViewItem>,
    isFirst: Boolean = false
) {

    val isAnim = !isFirst

    submitListAwait(viewItemList = viewItemList)

    if (isAnim) {
        transitionAwait()
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

    TransitionManager.go(Scene(this@transitionAwait), transition.addListener(transitionListener))

    awaitClose {
        transition.removeListener(transitionListener)
    }
}.first()