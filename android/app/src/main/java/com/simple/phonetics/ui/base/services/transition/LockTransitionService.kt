package com.simple.phonetics.ui.base.services.transition

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

interface LockTransitionService {

    var lockTransitionViewModel: LockTransitionViewModel

    fun setupTransitionLock(fragment: Fragment)

    fun setOnTransitionLockViewCreated(fragment: Fragment)
}

class LockTransitionServiceImpl : LockTransitionService {

    private var start = System.currentTimeMillis()

    override lateinit var lockTransitionViewModel: LockTransitionViewModel

    override fun setupTransitionLock(fragment: Fragment) {

        Log.d("tuanha", "setup: LockTransitionService ${fragment.javaClass.simpleName}")

        lockTransitionViewModel = fragment.viewModels<LockTransitionViewModel>().value

        setupLockRecord(fragment = fragment)
    }

    override fun setOnTransitionLockViewCreated(fragment: Fragment) {

        start = System.currentTimeMillis()

        lockTransitionViewModel.lockTransitionValue.distinctUntilChanged().launchCollect(fragment.viewLifecycleOwner) { isUnlock ->

            if (isUnlock) fragment.viewLifecycleOwner.lifecycleScope.launch {

                fragment.startPostponedEnterTransition()
            } else {

                fragment.postponeEnterTransition()
            }
        }

        setupLock(fragment = fragment)
    }

    private fun setupLock(fragment: Fragment) {

        val tag = fragment.javaClass.simpleName + "_setupLock"


        val view = fragment.view ?: return

        view.doOnLayout {

            lockTransitionViewModel.updateTransitionLock(tag + "_State", lock = true)
        }

        view.doOnPreDraw {

            lockTransitionViewModel.updateTransitionLock(tag + "_State", lock = false)
        }

        view.post {

            lockTransitionViewModel.updateTransitionLock(tag + "_State", lock = false)
        }
    }

    private fun setupLockRecord(fragment: Fragment) {

        lockTransitionViewModel.lockTransitionList.launchCollect(fragment) { list ->

            val time = System.currentTimeMillis() - start
            val isStart = list.all { !it.second }

            Log.d(
                "tuanha", "LockTransitionService ${fragment.javaClass.simpleName}  --->" +
                        "\ntime:${time}" +
                        "\nstartPostponedEnterTransition:${isStart}" +
                        "\nlocKList:${list.filter { it.second }}"
            )
        }
    }
}

class LockTransitionViewModel : ViewModel() {


    @VisibleForTesting
    var lockTransition = MutableLiveData(ConcurrentHashMap<String, Boolean>())

    val lockTransitionList = lockTransition.asFlow().map { it.toList() }.filter { it.isNotEmpty() }

    val lockTransitionValue = lockTransitionList.map { list -> list.all { !it.second } }


    init {

        lockTransitionValue.launchIn(viewModelScope)
    }

    fun lockTransition(tag: String) {

        updateTransitionLock(tag = tag, lock = true)
    }

    fun unlockTransition(tag: String) {

        updateTransitionLock(tag = tag, lock = false)
    }

    fun updateTransitionLock(tag: String, lock: Boolean) {

        val map = lockTransition.value ?: return

        map[tag] = lock

        lockTransition.value = map
    }

    suspend fun onTransitionLockEndAwait() {

        lockTransitionValue.filter { it }.first()
    }
}

fun Fragment.lockTransition(tag: String) {

    if (this is LockTransitionService) lockTransitionViewModel.lockTransition(tag = tag)
}

fun Fragment.unlockTransition(tag: String) {

    if (this is LockTransitionService) lockTransitionViewModel.unlockTransition(tag = tag)
}