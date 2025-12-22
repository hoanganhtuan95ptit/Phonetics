package com.simple.phonetics.ui.base.services.transition.running

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.simple.phonetics.BuildConfig
import com.simple.phonetics.ui.base.fragments.BaseFragment
import com.simple.phonetics.ui.base.fragments.LifecycleState.Companion.doCreated
import com.simple.phonetics.ui.base.fragments.LifecycleState.Companion.doDestroyed
import com.simple.phonetics.ui.base.fragments.LifecycleState.Companion.doPause
import com.simple.phonetics.ui.base.fragments.LifecycleState.Companion.doResumed
import com.unknown.coroutines.handler
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.Dispatchers

class RunningTransitionServiceImpl : RunningTransitionService {

    private lateinit var tagName: String

    override lateinit var runTransitionViewModel: RunTransitionViewModel
    override lateinit var runTransitionViewModelActivity: RunTransitionViewModel

    override fun setupTransitionRunning(fragment: BaseFragment<*, *>) {

        tagName = fragment.javaClass.simpleName.lowercase()

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

        val fragmentLifecycleTag = "fragmentLifecycle"

        fragment.stateFlow.launchCollect(fragment.lifecycleScope) {

            it.doCreated {
                startTransition(tag = fragmentLifecycleTag)
            }

            it.doResumed {
                endTransition(tag = fragmentLifecycleTag)
            }

            it.doPause {
                startTransition(tag = fragmentLifecycleTag)
            }

            it.doDestroyed {
                endTransition(tag = fragmentLifecycleTag)
            }
        }

        val fragmentViewLifecycleTag = "fragmentViewLifecycle"

        fragment.viewStateFlow.launchCollect(fragment.lifecycleScope) {

            it.doCreated {
                startTransition(tag = fragmentViewLifecycleTag)
            }

            it.doResumed {
                endTransition(tag = fragmentViewLifecycleTag)
            }

            it.doPause {
                startTransition(tag = fragmentViewLifecycleTag)
            }

            it.doDestroyed {
                endTransition(tag = fragmentViewLifecycleTag)
            }
        }
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
