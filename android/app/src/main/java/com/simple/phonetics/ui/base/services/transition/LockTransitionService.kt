package com.simple.phonetics.ui.base.services.transition

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.simple.coreapp.Param.ROOT_TRANSITION_NAME
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

interface LockTransitionService {

    var lockTransitionViewModel: LockTransitionViewModel

    fun setupTransitionLock(fragment: Fragment)
}

class LockTransitionServiceImpl : LockTransitionService {


    override lateinit var lockTransitionViewModel: LockTransitionViewModel


    private var start = System.currentTimeMillis()

    private var transitionName: String = ""


    override fun setupTransitionLock(fragment: Fragment) {

        lockTransitionViewModel = fragment.viewModels<LockTransitionViewModel>().value

        transitionName = fragment.arguments?.getString(ROOT_TRANSITION_NAME).toString()


        setupLock(fragment = fragment)
        setupLockQueue(fragment = fragment)
        setupLockRecord(fragment = fragment)
    }

    private fun setupLock(fragment: Fragment) = fragment.viewLifecycleOwnerLiveData.observe(fragment) {

        start = System.currentTimeMillis()

        val tag = fragment.javaClass.simpleName + "_setupLock"
        val view = fragment.view ?: return@observe

        lockTransitionViewModel.updateTransitionLock(tag + "_State", lock = true)

        view.doOnPreDraw {

            lockTransitionViewModel.updateTransitionLock(tag + "_State", lock = false)
        }

        view.post {

            lockTransitionViewModel.updateTransitionLock(tag + "_State", lock = false)
        }
    }

    private fun setupLockQueue(fragment: Fragment) = fragment.viewLifecycleOwnerLiveData.observe(fragment) {

        lockTransitionViewModel.lockTransitionValue.launchCollect(fragment.viewLifecycleOwner) { isUnlock ->


            fragment.onTransitionStatusEndAwait()// nếu transition đang run bị bỏ qua


            if (isUnlock) {

                fragment.startPostponedEnterTransition()
            } else {

                fragment.postponeEnterTransition()
            }
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