package com.simple.phonetics.ui.base.services.transition

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.simple.autobind.annotation.AutoBind
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.extentions.isActive
import com.simple.service.FragmentCreatedService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

@AutoBind(FragmentCreatedService::class)
class LockTransitionService : FragmentCreatedService {

    override fun setup(fragment: Fragment) {

        if (fragment !is Transition) return

        val viewModel = fragment.viewModels<LockTransitionViewModel>().value

        viewModel.lockTransition("STATE")


        val viewLifecycleOwner = fragment.viewLifecycleOwnerLiveData.asFlow()
        val lockTransitionStatus = viewModel.lockTransitionStatus


        combine(viewLifecycleOwner.filterNotNull(), lockTransitionStatus) { _, v -> v }.launchCollect(fragment) { start ->

            if (Transition.DEBUG) {
                Log.d("tuanha", "setup: LockTransitionService fragment:${fragment.javaClass.name} start:$start")
            }

            if (start) {

                fragment.startPostponedEnterTransition()
            } else {

                fragment.postponeEnterTransition()
            }
        }

        viewLifecycleOwner.launchCollect(fragment) {

            if (Transition.DEBUG) {
                Log.d("tuanha", "setup: LockTransitionService fragment:${fragment.javaClass.name} ${it != null}")
            }

            if (it != null) {

                viewModel.unlockTransition("STATE")
            } else {

                viewModel.lockTransition("STATE")
            }
        }


        record(fragment = fragment, viewModel = viewModel)
    }

    private fun record(fragment: Fragment, viewModel: LockTransitionViewModel) {

        val job = fragment.lifecycleScope.launch {

            var count = 0
            while (isActive()) {

                delay(500)
                count++

                if (Transition.DEBUG) {
                    Log.d("tuanha", "setup: LockTransitionService ${fragment.javaClass.name} count:$count ${viewModel.lockTransition.value}")
                }
            }
        }

        fragment.lifecycleScope.launch {

            viewModel.lockTransitionStatus.filter { it }.first()
            job.cancel()

            if (Transition.DEBUG) {
                Log.d("tuanha", "setup: LockTransitionService ${fragment.javaClass.name} end")
            }
        }

//        viewModel.lockTransition.asFlow().launchCollect(fragment) {
//
//            if (Transition.DEBUG) {
//                Log.d("tuanha", "setup: LockTransitionService ${fragment.javaClass.name}  $it")
//            }
//        }
    }
}

class LockTransitionViewModel : ViewModel() {

    var lockTransition = MutableLiveData(ConcurrentHashMap<String, Boolean>())

    val lockTransitionStatus = lockTransition.asFlow().map { map -> map.isNotEmpty() && map.values.all { it } }.distinctUntilChanged()


    fun lockTransition(vararg tag: String) = tag.forEach {

        lockTransition(it)
    }

    fun unlockTransition(vararg tag: String) = tag.forEach {

        unlockTransition(it)
    }

    fun lockTransition(tag: String) {

        val map = lockTransition.value ?: return

        map[tag] = false

        lockTransition.postValue(map)
    }

    fun unlockTransition(tag: String) {

        val map = lockTransition.value ?: return

        map[tag] = true

        lockTransition.postValue(map)
    }
}

fun Fragment.transitionLock() = viewModels<LockTransitionViewModel>().value

fun Fragment.lockTransition(tag: String) {

    transitionLock().lockTransition(tag = tag)
}

fun Fragment.unlockTransition(tag: String) {

    transitionLock().unlockTransition(tag = tag)
}