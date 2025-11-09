package com.simple.phonetics.ui.base.services.transition

import android.util.Log
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.simple.autobind.annotation.AutoBind
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.service.FragmentCreatedService
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap


@AutoBind(FragmentCreatedService::class)
class StateTransitionService : FragmentCreatedService {

    override fun setup(fragment: Fragment) {

        if (fragment !is Transition) return

        val tag = fragment.javaClass.name + "_StateTransitionService"
        val viewModel = fragment.viewModels<StateTransitionViewModel>().value


        fragment.viewLifecycleOwnerLiveData.asFlow().filterNotNull().launchCollect(fragment) {

            it.lifecycleScope.launch {

                it.repeatOnLifecycle(Lifecycle.State.STARTED) {

                    fragment.startTransition(tag)
                }
            }
        }

        fragment.viewLifecycleOwnerLiveData.asFlow().filterNotNull().launchCollect(fragment) {

            it.lifecycleScope.launch {

                it.repeatOnLifecycle(Lifecycle.State.RESUMED) {

                    fragment.endTransition(tag)
                }
            }
        }

        record(fragment = fragment, viewModel = viewModel)
    }

    private fun record(fragment: Fragment, viewModel: StateTransitionViewModel) {

        val start = System.currentTimeMillis()

        fragment.viewLifecycleOwnerLiveData.asFlow().filterNotNull().launchCollect(fragment) {

            fragment.onTransitionEndAwait()

            if (Transition.DEBUG) {
                Log.d("tuanha", "setup: StateTransitionService onTransitionEndAwait: ${System.currentTimeMillis() - start}")
            }
        }

//        viewModel.transition.asFlow().launchCollect(fragment) {
//
//            if (Transition.DEBUG) {
//                Log.d("tuanha", "setup: StateTransitionService ${fragment.javaClass.name}  $it")
//            }
//        }
    }
}

class StateTransitionViewModel : ViewModel() {

    val transition = MediatorLiveData(ConcurrentHashMap<String, Boolean>())

    val transitionStatus = transition.asFlow().filter { map -> map.isNotEmpty() && map.values.all { it } }

    fun updateTransitionState(tag: String, status: Boolean) {

        val map = transition.value ?: return
        map[tag] = status

        transition.postValue(map)
    }

    suspend fun onTransitionEndAwait() {

        transitionStatus.first()
    }
}

fun Fragment.endTransition(tag: String) {

    updateTransitionState(tag = tag, state = true)
}

fun Fragment.startTransition(tag: String) {

    updateTransitionState(tag = tag, state = false)
}

fun Fragment.updateTransitionState(tag: String, state: Boolean) {

    viewModels<StateTransitionViewModel>().value.updateTransitionState(tag = tag, status = state)
    activityViewModels<StateTransitionViewModel>().value.updateTransitionState(tag = tag, status = state)
}

suspend fun Fragment.onTransitionEndAwait() {

    viewModels<StateTransitionViewModel>().value.onTransitionEndAwait()
}

suspend fun FragmentActivity.onTransitionEndAwait() {

    viewModels<StateTransitionViewModel>().value.onTransitionEndAwait()
}