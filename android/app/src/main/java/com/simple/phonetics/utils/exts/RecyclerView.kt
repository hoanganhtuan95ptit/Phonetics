package com.simple.phonetics.utils.exts

import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.Scene
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.utils.extentions.submitListAwait
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.debounce
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


fun RecyclerView.listenerAdapterDataAsync() = channelFlow {

    val adapterDataObserver = object : RecyclerView.AdapterDataObserver() {

        override fun onChanged() {
            super.onChanged()
            trySend(Unit)
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
            trySend(Unit)
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            super.onItemRangeChanged(positionStart, itemCount, payload)
            trySend(Unit)
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            trySend(Unit)
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
            trySend(Unit)
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            trySend(Unit)
        }

        override fun onStateRestorationPolicyChanged() {
            super.onStateRestorationPolicyChanged()
            trySend(Unit)
        }
    }

    adapter?.registerAdapterDataObserver(adapterDataObserver)

    awaitClose {

        adapter?.unregisterAdapterDataObserver(adapterDataObserver)
    }
}.asLiveData().asFlow()

fun RecyclerView.listenerScrollAsync() = channelFlow {

    val listener = object : RecyclerView.OnScrollListener() {

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

            trySend(dx to dy)
        }
    }

    addOnScrollListener(listener)

    trySend(0f to 0f)

    awaitClose {

        removeOnScrollListener(listener)
    }
}.asLiveData().asFlow()

fun RecyclerView.listenerBindingAsync() = channelFlow {

    listenerAdapterDataAsync().launchCollect(this) {

        trySend(System.nanoTime())
    }

    listenerLayoutChangeAsync().launchCollect(this) {

        trySend(System.nanoTime())
    }

    awaitClose {

    }
}.asLiveData().asFlow()

suspend fun RecyclerView.submitListAndAwait(
    viewItemList: List<ViewItem>,
    isAnimation: Boolean = false
) {

    submitListAwait(viewItemList = viewItemList)

    if (isAnimation) {
        transitionAwait()
    }
}
