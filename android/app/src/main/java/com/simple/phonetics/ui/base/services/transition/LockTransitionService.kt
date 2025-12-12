package com.simple.phonetics.ui.base.services.transition

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.simple.coreapp.Param.ROOT_TRANSITION_NAME
import com.simple.phonetics.BuildConfig
import com.simple.phonetics.PhoneticsApp
import com.simple.phonetics.ui.base.fragments.BaseFragment
import com.unknown.coroutines.handler
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

interface LockTransitionService {

    var lockTransitionViewModel: LockTransitionViewModel

    fun setupTransitionLock(fragment: BaseFragment<*, *>)
}

class LockTransitionServiceImpl : LockTransitionService {


    override lateinit var lockTransitionViewModel: LockTransitionViewModel


    private var timeInit = System.currentTimeMillis()
    private var timeCreate = 0L
    private var timeCreateView = 0L

    private var isRecreateView = false
    private var isSupportEnterTransition = true


    override fun setupTransitionLock(fragment: BaseFragment<*, *>) {

        timeCreate = System.currentTimeMillis()

        lockTransitionViewModel = fragment.viewModels<LockTransitionViewModel>().value


        isSupportEnterTransition = fragment.arguments?.getString(ROOT_TRANSITION_NAME).orEmpty().isNotBlank()

        fragment.doObserver(object : DefaultLifecycleObserver {

            override fun onPause(owner: LifecycleOwner) {
                isRecreateView = true
            }
        })

        setupLock(fragment = fragment)
        setupLockQueue(fragment = fragment)
        setupLockRecord(fragment = fragment)
    }

    private fun setupLock(fragment: BaseFragment<*, *>) = fragment.viewLifecycleOwnerFlow.launchCollect(fragment) {

        timeCreateView = System.currentTimeMillis()

        val tag = fragment.javaClass.simpleName + "_setupLock"
        val view = fragment.view ?: return@launchCollect

        lockTransitionViewModel.updateTransitionLock(tag + "_State", isLock = true)

        view.doOnPreDraw {

            lockTransitionViewModel.updateTransitionLock(tag + "_State", isLock = false)
        }

        view.post {

            lockTransitionViewModel.updateTransitionLock(tag + "_State", isLock = false)
        }
    }

    private fun setupLockQueue(fragment: BaseFragment<*, *>) = fragment.viewLifecycleOwnerFlow.launchCollect(fragment) {

        lockTransitionViewModel.isUnlock.launchCollect(fragment.viewLifecycleOwner) { isUnlock ->


            if (!isSupportEnterTransition && !isRecreateView) {
                return@launchCollect
            }


            fragment.onTransitionStatusEndAwait()// nếu transition đang run bị bỏ qua


            if (isUnlock) {

                fragment.startPostponedEnterTransition()
            } else {

                fragment.postponeEnterTransition()
            }
        }
    }

    private fun setupLockRecord(fragment: Fragment) {

        lockTransitionViewModel.lockTransitionMap.map { list ->

            list.filter { it.value.isLock }
        }.distinctUntilChanged().launchCollect(fragment, context = handler + Dispatchers.IO) { map ->

            val isStart = map.isEmpty()

            if (BuildConfig.DEBUG && false) Log.d(
                "tuanha", "LockTransitionService ${fragment.javaClass.simpleName}  --->" +
                        "\ntimeAppInit:${System.currentTimeMillis() - PhoneticsApp.start.takeNowIfNotData()}" +
                        "\ntimeScreenInit:${System.currentTimeMillis() - timeInit.takeNowIfNotData()} -> space:${timeInit.takeNowIfNotData() - PhoneticsApp.start.takeNowIfNotData()}" +
                        "\ntimeCreate:${System.currentTimeMillis() - timeCreate.takeNowIfNotData()} -> space:${timeCreate.takeNowIfNotData() - timeInit.takeNowIfNotData()} " +
                        "\ntimeCreateView:${System.currentTimeMillis() - timeCreateView.takeNowIfNotData()} -> space:${timeCreateView.takeNowIfNotData() - timeCreate.takeNowIfNotData()}" +
                        "\nisRecreateView:${isRecreateView}" +
                        "\nisSupportEnterTransition:${isSupportEnterTransition}" +
                        "\nstartPostponedEnterTransition:${isStart}" +
                        "\nlocKList:${map.map { it.value.tag to (System.currentTimeMillis() - it.value.timeAdd) }}"
            )
        }
    }

    private fun Long.takeNowIfNotData() = takeIf { it != 0L } ?: System.currentTimeMillis()
}

class LockTransitionViewModel : ViewModel() {


    @VisibleForTesting
    var lockTransition = MutableLiveData(ConcurrentHashMap<String, LockInfo>())

    val lockTransitionMap = lockTransition.asFlow().filter { it.isNotEmpty() }

    val isUnlock = lockTransitionMap.map { map -> map.all { !it.value.isLock } }

    init {

        isUnlock.launchIn(viewModelScope)
    }

    fun lockTransition(tag: String) {

        updateTransitionLock(tag = tag, isLock = true)
    }

    fun unlockTransition(tag: String) {

        updateTransitionLock(tag = tag, isLock = false)
    }

    fun updateTransitionLock(tag: String, isLock: Boolean) {

        val map = lockTransition.value ?: return

        map[tag] = map[tag]?.copy(isLock = isLock) ?: LockInfo(tag = tag, isLock = isLock)

        lockTransition.value = map
    }

    suspend fun onTransitionLockEndAwait() {

        isUnlock.filter { it }.first()
    }

    data class LockInfo(
        val tag: String,
        val isLock: Boolean,
        val timeAdd: Long = System.currentTimeMillis()
    )
}


fun Fragment.lockTransition(tag: String) {

    if (this is LockTransitionService) lockTransitionViewModel.lockTransition(tag = tag)
}

fun Fragment.unlockTransition(tag: String) {

    if (this is LockTransitionService) lockTransitionViewModel.unlockTransition(tag = tag)
}