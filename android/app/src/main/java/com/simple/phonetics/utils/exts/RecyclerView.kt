package com.simple.phonetics.utils.exts

import androidx.core.view.children
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.simple.adapter.MultiAdapter
import com.simple.adapter.entities.ViewItem
import com.simple.core.utils.extentions.asObjectOrNull
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


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


fun RecyclerView.transitionAsync(transition: Transition = TransitionSet().addTransition(ChangeBounds().setDuration(350)).addTransition(Fade().setDuration(350))) = channelFlow {

    val layoutManagerCanDisableScroll = layoutManager.asObjectOrNull<CanDisableScroll>()

    val timeoutJob = launch {

        delay(350)
        trySend(Unit)
    }

    val transitionListener = object : Transition.TransitionListener {

        override fun onTransitionStart(transition: Transition) {
            layoutManagerCanDisableScroll?.isCanScroll = false
            timeoutJob.cancel()
        }

        override fun onTransitionEnd(transition: Transition) {
            layoutManagerCanDisableScroll?.isCanScroll = true
            trySend(Unit)
        }

        override fun onTransitionCancel(transition: Transition) {
            layoutManagerCanDisableScroll?.isCanScroll = true
            trySend(Unit)
        }

        override fun onTransitionPause(transition: Transition) {
            layoutManagerCanDisableScroll?.isCanScroll = true
        }

        override fun onTransitionResume(transition: Transition) {
            layoutManagerCanDisableScroll?.isCanScroll = false
        }
    }

    TransitionManager.beginDelayedTransition(this@transitionAsync, transition.addListener(transitionListener))

    awaitClose {
        transition.removeListener(transitionListener)
    }
}

fun RecyclerView.submitListAndGetListPositionChangeAsync(viewItemList: List<ViewItem>) = channelFlow {

    val adapter = (adapter as? MultiAdapter) ?: run {
        trySend(emptySet())
        awaitClose {}
        return@channelFlow
    }


    val positionChangeSet = mutableSetOf<Int>()

    // 3️⃣ Hàm kiểm tra item update giao với visible item
    fun updateIndexChange(positionStart: Int, itemCount: Int) {
        val updatedPositions = (positionStart until (positionStart + itemCount)).toSet()
        positionChangeSet.addAll(updatedPositions)
    }

    // 4️⃣ AdapterDataObserver
    val observer = object : RecyclerView.AdapterDataObserver() {

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            updateIndexChange(positionStart, itemCount)
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            updateIndexChange(positionStart, itemCount)
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            updateIndexChange(toPosition, itemCount)
            updateIndexChange(fromPosition, itemCount)
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            updateIndexChange(positionStart, itemCount)
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            updateIndexChange(positionStart, itemCount)
        }
    }

    adapter.registerAdapterDataObserver(observer)

    // 2️⃣ Submit list
    adapter.submitList(viewItemList) {

        trySend(positionChangeSet)
    }

    // 6️⃣ awaitClose để hủy observer khi coroutine hoàn tất
    awaitClose {
        adapter.unregisterAdapterDataObserver(observer)
    }
}


suspend fun RecyclerView.submitListAndAwait(viewItemList: List<ViewItem>, isAnimation: Boolean = false) {

    val visiblePositionSet = children.map { getChildAdapterPosition(it) }.toSet()
    val updatedPositionSet = submitListAndGetListPositionChangeAsync(viewItemList = viewItemList).first()


    val canChangeItemVisible = visiblePositionSet.isEmpty() || viewItemList.isEmpty() || updatedPositionSet.intersect(visiblePositionSet).isNotEmpty()

    if (canChangeItemVisible) if (isAnimation) {
        transitionAsync().first()
    } else {
        doOnPreDrawAsync().first()
    }
}

@Deprecated("use submitListAndAwait")
suspend fun RecyclerView.submitListAwaitV2(viewItemList: List<ViewItem>, isFromCache: Boolean = false) {

    submitListAndAwait(viewItemList = viewItemList, isAnimation = !isFromCache)
}