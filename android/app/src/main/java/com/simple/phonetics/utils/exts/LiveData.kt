package com.simple.phonetics.utils.exts

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.simple.adapter.provider.AdapterProvider
import com.simple.autobind.AutoBind
import com.simple.phonetics.ui.base.services.transition.lockTransition
import com.simple.phonetics.ui.base.services.transition.onTransitionRunningEndAwait
import com.simple.phonetics.ui.base.services.transition.unlockTransition
import com.unknown.coroutines.handler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val adapterFlow by lazy {
    AutoBind.loadNameAsync(AdapterProvider::class.java, false).distinctUntilChanged().asLiveData().asFlow()
}

/**
 * đợi có dữ liệu thì mới mở unlock hiệu ứng mở màn hình
 */
fun <T> LiveData<T>.collectWithLockTransitionUntilData(
    fragment: Fragment,
    tag: String = "",
    block: suspend (data: T) -> Unit
) = fragment.viewLifecycleOwner.lifecycleScope.launch(handler) {

    var isHasData = false

    fragment.lockTransition(tag = tag)

    asFlow().attachToAdapter().collect {

        if (isHasData) fragment.onTransitionRunningEndAwait()

        block(it)

        isHasData = true

        fragment.unlockTransition(tag = tag)
    }
}

/**
 * nếu đang có dữ liệu thì binding xong mới cho chạy hiệu ứng mở màn hình
 * nếu chưa có dữ liệu thì đợi cho hiệu ứng mở màn hình chạy xong mới binding
 */
fun <T> LiveData<T>.collectWithLockTransitionIfCached(
    fragment: Fragment,
    tag: String = "",
    block: suspend (data: T, isFromCache: Boolean) -> Unit
) = fragment.viewLifecycleOwner.lifecycleScope.launch(handler) {

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

            fragment.onTransitionRunningEndAwait()

            block(it, false)
        }

        data = null
    }
}

fun <T> Flow<T>.attachToAdapter(): Flow<T> = combine(
    this,
    adapterFlow
) { d, _ ->
    d
}