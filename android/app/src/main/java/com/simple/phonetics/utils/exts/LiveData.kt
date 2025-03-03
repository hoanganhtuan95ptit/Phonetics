package com.simple.phonetics.utils.exts

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.simple.coreapp.ui.base.fragments.transition.TransitionFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun <T> LiveData<T>.observeWithTransition(
    fragment: TransitionFragment<*, *>,
    owner: LifecycleOwner,
    tag: String = "",
    block: (data: T) -> Unit
) {

    fragment.lockTransition(tag = tag)

    observe(owner) {

        block(it)

        fragment.unlockTransition(tag = tag)
    }
}

fun <T> LiveData<T>.observeWithTransitionV2(
    fragment: TransitionFragment<*, *>,
    owner: LifecycleOwner,
    tag: String = "",
    block: suspend (data: T, isFirst: Boolean) -> Unit
) = owner.lifecycleScope.launch {

    var data = value

    if (data != null) {

        fragment.lockTransition(tag = tag)

        block(data, true)

        fragment.unlockTransition(tag = tag)
    }

    asFlow().collect {

        val diff = data == null || withContext(Dispatchers.IO) {
            data != it
        }

        if (diff){

            fragment.viewModel.awaitTransition()

            block(it, false)
        }

        data = null
    }
}