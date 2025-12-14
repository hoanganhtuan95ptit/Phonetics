package com.simple.phonetics.ui.base.services.transition.locking

import android.util.Log
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.simple.phonetics.BuildConfig
import com.simple.phonetics.Param.ROOT_TRANSITION_NAME
import com.simple.phonetics.ui.base.fragments.BaseFragment
import com.simple.phonetics.ui.base.services.transition.doObserver
import com.simple.phonetics.ui.base.services.transition.onTransitionStatusEndAwait
import com.unknown.coroutines.handler
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.Dispatchers

class LockingTransitionServiceImpl : LockingTransitionService {


    override lateinit var lockingTransitionViewModel: LockingTransitionViewModel
    override lateinit var lockingTransitionViewModelActivity: LockingTransitionViewModel


    private var isRecreateView = false
    private var isSupportEnterTransition = true


    override fun setupTransitionLock(fragment: BaseFragment<*, *>) {

        lockingTransitionViewModel = fragment.viewModels<LockingTransitionViewModel>().value
        lockingTransitionViewModelActivity = fragment.activityViewModels<LockingTransitionViewModel>().value

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

    override fun lockTransition(tag: String) {

        lockingTransitionViewModel.lockTransition(tag)
        lockingTransitionViewModelActivity.lockTransition(tag)
    }

    override fun unlockTransition(tag: String) {

        lockingTransitionViewModel.unlockTransition(tag)
        lockingTransitionViewModelActivity.unlockTransition(tag)
    }

    private fun setupLock(fragment: BaseFragment<*, *>) = fragment.viewLifecycleOwnerFlow.launchCollect(fragment) {

        val tag = fragment.javaClass.simpleName + "_setupLock"
        val view = fragment.view ?: return@launchCollect

        lockTransition(tag + "_State")

        view.doOnPreDraw {

            unlockTransition(tag + "_State")
        }

        view.post {

            unlockTransition(tag + "_State")
        }
    }

    private fun setupLockQueue(fragment: BaseFragment<*, *>) = fragment.viewLifecycleOwnerFlow.launchCollect(fragment) {

        lockingTransitionViewModel.isUnlock.launchCollect(fragment.viewLifecycleOwner) { isUnlock ->


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

        lockingTransitionViewModel.lockingData.launchCollect(fragment, context = handler + Dispatchers.IO) { info ->

            if (BuildConfig.DEBUG && false) Log.d(
                "tuanha", "LockingTransitionServiceFragment  ${fragment.javaClass.simpleName}  --->" +
                        "\nisRecreateView:${isRecreateView}" +
                        "\nisSupportEnterTransition:${isSupportEnterTransition}" +
                        "\nstartPostponedEnterTransition:${info.isStart}" +
                        "\nlocKList:${info.data.map { it.value.tag to (System.currentTimeMillis() - it.value.timeAdd) }}"
            )
        }
    }
}
