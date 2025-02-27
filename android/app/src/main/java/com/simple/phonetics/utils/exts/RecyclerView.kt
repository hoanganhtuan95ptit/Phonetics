package com.simple.phonetics.utils.exts

import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionSet
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.ui.base.fragments.transition.TransitionFragment
import com.simple.coreapp.utils.extentions.beginTransitionAwait
import com.simple.coreapp.utils.extentions.submitListAwait


suspend fun RecyclerView.submitListAwait(
    transitionFragment: TransitionFragment<*, *>,
    viewItemList: List<ViewItem>?,
    isFirst: Boolean = false,
    tag: String = ""
) {

    val isAnim = !isFirst && viewItemList != null

    if (isFirst) {
        transitionFragment.lockTransition(tag = tag)
    }

    if (isAnim) {
        transitionFragment.viewModel.awaitTransition()
    } else {
        transitionFragment.unlockTransition(tag = tag)
    }

    if (viewItemList != null) {
        submitListAwait(viewItemList = viewItemList)
    }

    if (isAnim) {
        val transition = TransitionSet().addTransition(ChangeBounds().setDuration(350)).addTransition(Fade().setDuration(350))
        beginTransitionAwait(transition)
    } else {
        transitionFragment.unlockTransition(tag = tag)
    }
}