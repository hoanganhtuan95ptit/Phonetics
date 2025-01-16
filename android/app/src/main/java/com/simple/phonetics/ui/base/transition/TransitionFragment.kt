package com.simple.phonetics.ui.base.transition

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import com.simple.coreapp.ui.base.fragments.BaseViewModelFragment
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.phonetics.Param
import com.simple.state.ResultState
import com.simple.state.isSuccess
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getActivityViewModel

abstract class TransitionFragment<T : androidx.viewbinding.ViewBinding, VM : TransitionViewModel>(@androidx.annotation.LayoutRes contentLayoutId: Int = 0) : BaseViewModelFragment<T, VM>(contentLayoutId) {


    private lateinit var lockTransition: MediatorLiveData<HashMap<String, ResultState<*>>>

    private val activityViewModel: TransitionGlobalViewModel by lazy {
        getActivityViewModel()
    }

    private var fromCreate: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isAnim = arguments != null && requireArguments().containsKey(Param.ROOT_TRANSITION_NAME)

        fromCreate = !isAnim
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

            if (!fromCreate) if (start) {

                startPostponedEnterTransition()
            } else {

                postponeEnterTransition()
            }
        }

        lockTransition(STATE)

        viewLifecycleOwner.lifecycleScope.launch {

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

    override fun onDestroyView() {
        super.onDestroyView()
        fromCreate = false
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

        val isAnim = arguments != null && arguments.containsKey(Param.ROOT_TRANSITION_NAME)
        val transitionDuration = 350L

        exitTransition = Hold().apply {

            duration = transitionDuration
        }

        returnTransition = Hold().apply {

            duration = transitionDuration
        }

        reenterTransition = Hold().apply {

            duration = transitionDuration
        }

        sharedElementReturnTransition = MaterialContainerTransform(requireContext(), true).apply {

            duration = transitionDuration

            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH

            setPathMotion(MaterialArcMotion())

            interpolator = FastOutSlowInInterpolator()
            scrimColor = Color.TRANSPARENT
        }

        if (isAnim) sharedElementEnterTransition = MaterialContainerTransform(requireContext(), true).apply {

            duration = transitionDuration

            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH

            setPathMotion(MaterialArcMotion())

            interpolator = FastOutSlowInInterpolator()
            scrimColor = Color.TRANSPARENT
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