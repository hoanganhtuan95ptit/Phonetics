package com.simple.phonetics.ui.base

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.transition.Transition
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import com.simple.coreapp.ui.base.fragments.BaseViewModelFragment
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.phonetics.Param
import com.simple.state.ResultState
import com.simple.state.isSuccess
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getActivityViewModel

abstract class TransitionFragment<T : androidx.viewbinding.ViewBinding, VM : TransitionViewModel>(@androidx.annotation.LayoutRes contentLayoutId: Int = 0) : BaseViewModelFragment<T, VM>(contentLayoutId) {

    private lateinit var lockTransition: MediatorLiveData<HashMap<String, ResultState<*>>>

    private val activityViewModel: TransitionGlobalViewModel by lazy {
        getActivityViewModel()
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lockTransition = MediatorLiveData(hashMapOf())

        super.onViewCreated(view, savedInstanceState)

        val arguments = arguments

        arguments?.getString(Param.ROOT_TRANSITION_NAME)?.let {

            view.transitionName = it
        }

        lockTransition.asFlow().map { map -> map.isNotEmpty() && map.values.all { it.isSuccess() } }.distinctUntilChanged().launchCollect(viewLifecycleOwner) { start ->

            if (start) {

                setTransitionAnimation()
            }

            if (start) {

                startPostponedEnterTransition()
            } else {

                postponeEnterTransition()
            }
        }

        lockTransition(STATE)

        viewLifecycleOwner.lifecycleScope.launch {

            delay(100)
            unlockTransition(STATE)
        }
    }

    fun lockTransition(vararg tag: String) = tag.forEach {

        lockTransition(it)
    }

    fun unlockTransition(vararg tag: String) = tag.forEach {

        unlockTransition(it)
    }

    fun lockTransition(tag: String) {

        val map = lockTransition.value ?: return

        map[tag] = ResultState.Start

        lockTransition.postValue(map)
    }

    fun unlockTransition(tag: String) {

        val map = lockTransition.value ?: return

        map[tag] = ResultState.Success("")

        lockTransition.postValue(map)
    }

    private fun setTransitionAnimation() {

        val arguments = arguments

        val isFistScreen = arguments != null && arguments.containsKey(Param.TRANSITION_DURATION) && arguments.getLong(Param.TRANSITION_DURATION) == 0L
        val transitionDuration = 350L

        enterTransition = Hold().apply {

            duration = if (isFistScreen) 0 else transitionDuration
        }.addListener(getTransitionListener("enterTransition"))

        exitTransition = Hold().apply {

            duration = transitionDuration
        }.addListener(getTransitionListener("exitTransition"))

        returnTransition = Hold().apply {

            duration = transitionDuration
        }.addListener(getTransitionListener("returnTransition"))

        reenterTransition = Hold().apply {

            duration = transitionDuration
        }.addListener(getTransitionListener("reenterTransition"))


        sharedElementReturnTransition = MaterialContainerTransform(requireContext(), true).apply {

            duration = transitionDuration

            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH

            setPathMotion(MaterialArcMotion())

            interpolator = FastOutSlowInInterpolator()
            scrimColor = Color.TRANSPARENT
        }.addListener(getTransitionListener("sharedElementReturnTransition"))

        sharedElementEnterTransition = MaterialContainerTransform(requireContext(), true).apply {

            duration = if (isFistScreen) 0 else transitionDuration

            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH

            setPathMotion(MaterialArcMotion())

            interpolator = FastOutSlowInInterpolator()
            scrimColor = Color.TRANSPARENT
        }.addListener(getTransitionListener("sharedElementEnterTransition"))
    }

    private fun getTransitionListener(name: String) = object : Transition.TransitionListener {

        private var timeoutJob: Job? = null

        init {

            viewModel.transitionState(name, ResultState.Start)
            activityViewModel.transitionState(name, ResultState.Start)

            timeoutJob = lifecycleScope.launch {

                delay(100)

//                Log.d("tuanha", "getTransitionListener:${this@TransitionFragment.javaClass.simpleName} $name timeout")
                viewModel.transitionState(name, ResultState.Success(""))
                activityViewModel.transitionState(name, ResultState.Success(""))
            }
        }

        override fun onTransitionStart(transition: Transition) {

//            Log.d("tuanha", "getTransitionListener:${this@TransitionFragment.javaClass.simpleName} $name onTransitionStart")

            timeoutJob?.cancel()
        }

        override fun onTransitionEnd(transition: Transition) {

//            Log.d("tuanha", "getTransitionListener:${this@TransitionFragment.javaClass.simpleName} $name onTransitionEnd")

            viewModel.transitionState(name, ResultState.Success(""))
            activityViewModel.transitionState(name, ResultState.Success(""))
        }

        override fun onTransitionCancel(transition: Transition) {

//            Log.d("tuanha", "getTransitionListener:${this@TransitionFragment.javaClass.simpleName} $name onTransitionCancel")

            viewModel.transitionState(name, ResultState.Success(""))
            activityViewModel.transitionState(name, ResultState.Success(""))
        }

        override fun onTransitionPause(transition: Transition) {

//            Log.d("tuanha", "getTransitionListener:${this@TransitionFragment.javaClass.simpleName} $name onTransitionPause")
        }

        override fun onTransitionResume(transition: Transition) {

//            Log.d("tuanha", "getTransitionListener:${this@TransitionFragment.javaClass.simpleName} $name onTransitionResume")
        }
    }

    companion object {

        private const val STATE = "STATE"
    }
}