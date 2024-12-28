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

        lockTransition.asFlow().map { map ->

            map.isNotEmpty() && map.values.all { it.isSuccess() }
        }.distinctUntilChanged().launchCollect(viewLifecycleOwner) { start ->

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

    override fun onResume() {
        super.onResume()
        updateState(STATE, ResultState.Success(""))
    }

    override fun onPause() {
        super.onPause()
        updateState(STATE, ResultState.Start)
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

        override fun onTransitionStart(transition: Transition) {
        }

        override fun onTransitionEnd(transition: Transition) {
        }

        override fun onTransitionCancel(transition: Transition) {
        }

        override fun onTransitionPause(transition: Transition) {
        }

        override fun onTransitionResume(transition: Transition) {
        }
    }

    private fun updateState(name: String, state: ResultState<String>) {

        viewModel.transitionState(name, state)
        activityViewModel.transitionState(name, state)
    }

    companion object {

        private const val STATE = "STATE"
    }
}