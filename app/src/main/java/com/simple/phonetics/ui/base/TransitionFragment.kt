package com.simple.phonetics.ui.base

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import androidx.transition.Transition
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialElevationScale
import com.simple.coreapp.TRANSITION_DURATION
import com.simple.coreapp.ui.base.fragments.BaseViewModelFragment
import com.simple.phonetics.Param
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class TransitionFragment<T : androidx.viewbinding.ViewBinding, VM : TransitionViewModel>(@androidx.annotation.LayoutRes contentLayoutId: Int = 0) : BaseViewModelFragment<T, VM>(contentLayoutId) {

    var timeoutJob: Job? = null

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString(Param.ROOT_TRANSITION_NAME)?.let {

            view.transitionName = it
        }

        getTransitionTimeoutJob()

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
    }

    override fun postponeEnterTransition() {

        timeoutJob?.cancel()

        super.postponeEnterTransition()
    }

    override fun startPostponedEnterTransition() {

        getTransitionTimeoutJob()

        super.startPostponedEnterTransition()
    }

    private fun getTransitionTimeoutJob() {

        timeoutJob?.cancel()
        timeoutJob = lifecycleScope.launch {

            delay(100)
            viewModel.transitionEnd.postValue(true)
        }
    }

    private fun getTransitionListener(name: String) = object : Transition.TransitionListener {

        override fun onTransitionStart(transition: Transition) {

//            Log.d("tuanha", "onTransitionStart: $name onTransitionStart ${this@TransitionFragment.javaClass.simpleName}")

            timeoutJob?.cancel()
        }

        override fun onTransitionEnd(transition: Transition) {

//            Log.d("tuanha", "onTransitionStart: $name onTransitionEnd ${this@TransitionFragment.javaClass.simpleName}")

            viewModel.transitionEnd.postValue(true)
        }

        override fun onTransitionCancel(transition: Transition) {

//            Log.d("tuanha", "onTransitionStart: $name onTransitionCancel ${this@TransitionFragment.javaClass.simpleName}")

            viewModel.transitionEnd.postValue(true)
        }

        override fun onTransitionPause(transition: Transition) {


        }

        override fun onTransitionResume(transition: Transition) {


        }
    }
}