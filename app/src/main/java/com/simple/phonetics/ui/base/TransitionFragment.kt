package com.simple.phonetics.ui.base

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.CallSuper
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.transition.Transition
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialElevationScale
import com.simple.core.utils.extentions.toJson
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

abstract class TransitionFragment<T : androidx.viewbinding.ViewBinding, VM : TransitionViewModel>(@androidx.annotation.LayoutRes contentLayoutId: Int = 0) : BaseViewModelFragment<T, VM>(contentLayoutId) {

    private lateinit var lockTransition: MediatorLiveData<HashMap<String, ResultState<*>>>


    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lockTransition = MediatorLiveData(hashMapOf())

        super.onViewCreated(view, savedInstanceState)

        val TRANSITION_DURATION = 500L

        arguments?.getString(Param.ROOT_TRANSITION_NAME)?.let {

            view.transitionName = it
        }

        enterTransition = MaterialElevationScale(false).apply {

            duration = TRANSITION_DURATION
        }.addListener(getTransitionListener("enterTransition"))

        exitTransition = MaterialElevationScale(false).apply {

            duration = TRANSITION_DURATION
        }.addListener(getTransitionListener("exitTransition"))

        returnTransition = MaterialElevationScale(false).apply {

            duration = TRANSITION_DURATION
        }.addListener(getTransitionListener("returnTransition"))

        reenterTransition = MaterialElevationScale(false).apply {

            duration = TRANSITION_DURATION
        }.addListener(getTransitionListener("reenterTransition"))


        sharedElementReturnTransition = MaterialContainerTransform(requireContext(), true).apply {

            duration = TRANSITION_DURATION

            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH

            setPathMotion(MaterialArcMotion())

            interpolator = FastOutSlowInInterpolator()
            scrimColor = Color.TRANSPARENT
        }.addListener(getTransitionListener("sharedElementReturnTransition"))

        sharedElementEnterTransition = MaterialContainerTransform(requireContext(), true).apply {

            duration = TRANSITION_DURATION

            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH

            setPathMotion(MaterialArcMotion())

            interpolator = FastOutSlowInInterpolator()
            scrimColor = Color.TRANSPARENT
        }.addListener(getTransitionListener("sharedElementEnterTransition"))

        lockTransition.asFlow().map { map ->

            map.isNotEmpty() && map.values.all { it.isSuccess() }
        }.distinctUntilChanged().launchCollect(viewLifecycleOwner) { start ->

            if (start) {

                startPostponedEnterTransition()
            }
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

        postponeEnterTransition()
    }

    fun unlockTransition(tag: String) {

        val map = lockTransition.value ?: return

        map[tag] = ResultState.Success("")

        lockTransition.postValue(map)
    }

    private fun getTransitionListener(name: String) = object : Transition.TransitionListener {

        private var timeoutJob: Job? = null

        init {

            viewModel.transitionState(name, ResultState.Start)

            timeoutJob = lifecycleScope.launch {

                delay(100)
                viewModel.transitionState(name, ResultState.Success(""))
            }
        }

        override fun onTransitionStart(transition: Transition) {

//            Log.d("tuanha", "getTransitionListener: $name onTransitionStart ${this@TransitionFragment.javaClass.simpleName}")

            timeoutJob?.cancel()
        }

        override fun onTransitionEnd(transition: Transition) {

//            Log.d("tuanha", "getTransitionListener: $name onTransitionEnd ${this@TransitionFragment.javaClass.simpleName}")

            viewModel.transitionState(name, ResultState.Success(""))
        }

        override fun onTransitionCancel(transition: Transition) {

//            Log.d("tuanha", "getTransitionListener: $name onTransitionCancel ${this@TransitionFragment.javaClass.simpleName}")

            viewModel.transitionState(name, ResultState.Success(""))
        }

        override fun onTransitionPause(transition: Transition) {

//            Log.d("tuanha", "getTransitionListener: $name onTransitionPause ${this@TransitionFragment.javaClass.simpleName}")
        }

        override fun onTransitionResume(transition: Transition) {

//            Log.d("tuanha", "getTransitionListener: $name onTransitionResume ${this@TransitionFragment.javaClass.simpleName}")
        }
    }
}