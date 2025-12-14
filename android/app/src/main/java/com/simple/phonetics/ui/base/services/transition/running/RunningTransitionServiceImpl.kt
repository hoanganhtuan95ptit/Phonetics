package com.simple.phonetics.ui.base.services.transition.running

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.simple.phonetics.BuildConfig
import com.simple.phonetics.ui.base.fragments.BaseFragment
import com.simple.phonetics.ui.base.services.transition.doObserver
import com.simple.phonetics.ui.base.services.transition.launchWithResumed
import com.unknown.coroutines.handler
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.Dispatchers

class RunningTransitionServiceImpl : RunningTransitionService {

    private lateinit var tagName: String

    override lateinit var runTransitionViewModel: RunTransitionViewModel
    override lateinit var runTransitionViewModelActivity: RunTransitionViewModel

    override fun setupTransitionRunning(fragment: BaseFragment<*, *>) {

        tagName = this.javaClass.simpleName.lowercase()

        runTransitionViewModel = fragment.viewModels<RunTransitionViewModel>().value
        runTransitionViewModelActivity = fragment.activityViewModels<RunTransitionViewModel>().value


        setupRunning(fragment = fragment)
        setupRunningTracking(fragment = fragment)
    }

    override fun endTransition(tag: String) {

        runTransitionViewModel.endTransition(tag = "${tagName}_$tag")
        runTransitionViewModelActivity.endTransition(tag = "${tagName}_$tag")
    }

    override fun startTransition(tag: String) {

        runTransitionViewModel.startTransition(tag = "${tagName}_$tag")
        runTransitionViewModelActivity.startTransition(tag = "${tagName}_$tag")
    }

    private fun setupRunning(fragment: BaseFragment<*, *>) {

        val tag = "setupRunning"

        startTransition(tag + "_state")

        fragment.viewLifecycleOwnerFlow.launchCollect(fragment) {

            startTransition(tag + "_state")

            it.launchWithResumed {

                endTransition(tag + "_state")
            }
        }


        fragment.doObserver(object : DefaultLifecycleObserver {

            override fun onResume(owner: LifecycleOwner) {

                endTransition(tag + "_lifecycle")
            }

            override fun onPause(owner: LifecycleOwner) {

                startTransition(tag + "_lifecycle")
            }

            override fun onDestroy(owner: LifecycleOwner) {

                endTransition(tag + "_lifecycle")
            }
        })
    }

    private fun setupRunningTracking(fragment: Fragment) {

        runTransitionViewModel.lockingData.launchCollect(fragment, context = handler + Dispatchers.IO) { info ->

            if (BuildConfig.DEBUG && false) Log.d(
                "tuanha", "RunningTransitionService ${fragment.javaClass.simpleName}  --->" +
                        "\nstartPostponedEnterTransition:${info.isRunning}" +
                        "\nrunningList:${info.data.map { it.value.tag to (System.currentTimeMillis() - it.value.timeAdd) }}"
            )
        }
    }
}
