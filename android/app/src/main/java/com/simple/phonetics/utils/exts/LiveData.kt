package com.simple.phonetics.utils.exts

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.simple.adapter.utils.exts.attachToAdapter
import com.simple.coreapp.ui.base.fragments.transition.TransitionFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun <T> LiveData<T>.collectWithLockTransitionUntilData(
    fragment: TransitionFragment<*, *>,
    tag: String = "",
    block: suspend (data: T) -> Unit
) = fragment.viewLifecycleOwner.lifecycleScope.launch {

    fragment.lockTransition(tag = tag)

    asFlow().attachToAdapter().collect {

        block(it)

        fragment.unlockTransition(tag = tag)
    }
}

fun <T> LiveData<T>.collectWithLockTransitionIfCached(
    fragment: TransitionFragment<*, *>,
    tag: String = "",
    block: suspend (data: T, isFirst: Boolean) -> Unit
) = fragment.viewLifecycleOwner.lifecycleScope.launch {

    var data = value

    if (data != null) {

        fragment.lockTransition(tag = tag)

        block(data, true)

        fragment.unlockTransition(tag = tag)
    }

    asFlow().attachToAdapter().collect {

        val diff = data == null || withContext(Dispatchers.IO) {
            data != it
        }

        if (diff) {

            fragment.viewModel.awaitTransition()

            block(it, false)
        }

        data = null
    }
}