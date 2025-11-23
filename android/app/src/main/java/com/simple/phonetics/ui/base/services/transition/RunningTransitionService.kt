package com.simple.phonetics.ui.base.services.transition

import android.util.Log
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.simple.phonetics.BuildConfig
import com.unknown.coroutines.handler
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

interface RunningTransitionService {

    var runTransitionViewModel: RunTransitionViewModel
    var runTransitionViewModelActivity: RunTransitionViewModel

    fun setupTransitionRunning(fragment: Fragment)
}

class RunningTransitionServiceImpl : RunningTransitionService {

    override lateinit var runTransitionViewModel: RunTransitionViewModel
    override lateinit var runTransitionViewModelActivity: RunTransitionViewModel

    override fun setupTransitionRunning(fragment: Fragment) {

        runTransitionViewModel = fragment.viewModels<RunTransitionViewModel>().value
        runTransitionViewModelActivity = fragment.activityViewModels<RunTransitionViewModel>().value


        setupRunning(fragment = fragment)
        setupRunningRecord(fragment = fragment)
    }

    private fun setupRunning(fragment: Fragment) {

        val tag = fragment.javaClass.simpleName + "_setupRunning"


        fragment.updateTransitionRunning(tag + "_State", isRunning = true)

        fragment.viewLifecycleOwnerLiveData.observe(fragment) {

            fragment.updateTransitionRunning(tag + "_State", isRunning = true)

            it.launchWithResumed {

                fragment.updateTransitionRunning(tag + "_State", isRunning = false)
            }
        }


        fragment.doObserver(object : DefaultLifecycleObserver {

            override fun onResume(owner: LifecycleOwner) {

                fragment.updateTransitionRunning(tag + "_Lifecycle", isRunning = false)
            }

            override fun onPause(owner: LifecycleOwner) {

                fragment.updateTransitionRunning(tag + "_Lifecycle", isRunning = true)
            }

            override fun onDestroy(owner: LifecycleOwner) {

                fragment.updateTransitionRunning(tag + "_Lifecycle", isRunning = false)
            }
        })
    }

    private fun setupRunningRecord(fragment: Fragment) {

        var start = System.currentTimeMillis()

        fragment.viewLifecycleOwnerLiveData.observe(fragment) {

            start = System.currentTimeMillis()
        }

        runTransitionViewModel.runningTransitionList.launchCollect(fragment, context = handler + Dispatchers.IO) { map ->

            val time = System.currentTimeMillis() - start
            val isRunning = map.filter { it.first.contains("Transition_", true) }.any { it.second }

            if (BuildConfig.DEBUG && false) Log.d(
                "tuanha", "RunningTransitionService runTransitionViewModel ${fragment.javaClass.simpleName}  --->" +
                        "\ntime:${time}" +
                        "\nisRunning:${isRunning}" +
                        "\nrunningList:${map.filter { it.second }}"
            )
        }

        runTransitionViewModelActivity.runningTransitionList.launchCollect(fragment, context = handler + Dispatchers.IO) { map ->

            val time = System.currentTimeMillis() - start
            val isRunning = map.filter { it.first.contains("Transition_", true) }.any { it.second }

            if (BuildConfig.DEBUG && false) Log.d(
                "tuanha", "RunningTransitionService runTransitionViewModelActivity ${fragment.javaClass.simpleName}  --->" +
                        "\ntime:${time}" +
                        "\nisRunning:${isRunning}" +
                        "\nrunningList:${map.filter { it.second }}"
            )
        }
    }
}

class RunTransitionViewModel : ViewModel() {


    @VisibleForTesting
    val runningTransition = MutableLiveData(ConcurrentHashMap<String, Boolean>())

    val runningTransitionList = runningTransition.asFlow().map { it.toList() }.filter { it.isNotEmpty() }

    val runningTransitionValue = runningTransitionList.map { list -> list.all { !it.second } }


    init {

        runningTransitionValue.launchIn(viewModelScope)
    }

    fun updateTransitionRunning(tag: String, isRunning: Boolean) {

        val map = runningTransition.value ?: return

        map[tag] = isRunning

        runningTransition.value = map
    }

    suspend fun onTransitionRunningEndAwait() {

        runningTransitionValue.filter { it }.first()
    }
}

fun Fragment.endTransition(tag: String) {

    updateTransitionRunning(tag = tag, isRunning = false)
}

fun Fragment.startTransition(tag: String) {

    updateTransitionRunning(tag = tag, isRunning = true)
}

fun Fragment.updateTransitionRunning(tag: String, isRunning: Boolean) {

    if (this is RunningTransitionService) runTransitionViewModel.updateTransitionRunning(tag = tag, isRunning = isRunning)
    if (this is RunningTransitionService) runTransitionViewModelActivity.updateTransitionRunning(tag = tag, isRunning = isRunning)
}

suspend fun Fragment.onTransitionRunningEndAwait() {

    if (this is RunningTransitionService) runTransitionViewModel.onTransitionRunningEndAwait()
}

suspend fun FragmentActivity.onTransitionRunningEndAwait() {

    viewModels<RunTransitionViewModel>().value.onTransitionRunningEndAwait()
}