package com.simple.phonetics.ui.base.services.transition

import android.graphics.Color
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.asFlow
import androidx.transition.Transition
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import com.simple.autobind.annotation.AutoBind
import com.simple.coreapp.Param
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.service.FragmentCreatedService
import kotlinx.coroutines.flow.filterNotNull


@AutoBind(FragmentCreatedService::class)
class TransitionService : FragmentCreatedService {


    private val transitionDuration = 350L
    private var isCanUseTransition: Boolean = false


    override fun setup(fragment: Fragment) {

        if (fragment !is com.simple.phonetics.ui.base.services.transition.Transition) return

        val transitionName = fragment.arguments?.getString(Param.ROOT_TRANSITION_NAME)

        if (com.simple.phonetics.ui.base.services.transition.Transition.DEBUG){
            Log.d("tuanha", "setup: ${fragment.javaClass.simpleName} transitionName:$transitionName")
        }

        isCanUseTransition = !transitionName.isNullOrBlank()


        setTransitionAnimation(fragment = fragment)


        fragment.viewLifecycleOwnerLiveData.asFlow().filterNotNull().launchCollect(fragment) {

            fragment.view?.transitionName = transitionName
        }
    }


    private fun setTransitionAnimation(fragment: Fragment) {

        fragment.exitTransition = Hold().apply {

            duration = transitionDuration
        }.apply {

            addListener(DefaultTransitionListener(name = "exitTransition", fragment = fragment))
        }

        fragment.reenterTransition = Hold().apply {

            duration = transitionDuration
        }.apply {

            addListener(DefaultTransitionListener(name = "reenterTransition", fragment = fragment))
        }

        if (isCanUseTransition) {

            customEnterTransition(fragment = fragment)
            customReturnTransition(fragment = fragment)
        }
    }

    private fun customReturnTransition(fragment: Fragment) {

        fragment.sharedElementReturnTransition = MaterialContainerTransform(fragment.requireContext(), true).apply {

            duration = transitionDuration

            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH

            setPathMotion(MaterialArcMotion())

            interpolator = FastOutSlowInInterpolator()
            scrimColor = Color.TRANSPARENT
        }.apply {

            addListener(DefaultTransitionListener(name = "sharedElementReturnTransition", fragment = fragment))
        }
    }

    private fun customEnterTransition(fragment: Fragment) {

        fragment.sharedElementEnterTransition = MaterialContainerTransform(fragment.requireContext(), true).apply {

            duration = transitionDuration

            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH

            setPathMotion(MaterialArcMotion())

            interpolator = FastOutSlowInInterpolator()
            scrimColor = Color.TRANSPARENT
        }.apply {

            addListener(DefaultTransitionListener(name = "sharedElementEnterTransition", fragment = fragment))
        }
    }

    private class DefaultTransitionListener(val name: String, val fragment: Fragment) : Transition.TransitionListener {

        override fun onTransitionStart(transition: Transition) {
            fragment.startTransition("${fragment.javaClass.name}_${name}_START")
        }

        override fun onTransitionEnd(transition: Transition) {
            fragment.endTransition("${fragment.javaClass.name}_${name}_START")
        }

        override fun onTransitionCancel(transition: Transition) {
            fragment.endTransition("${fragment.javaClass.name}_${name}_START")
        }


        override fun onTransitionPause(transition: Transition) {
            fragment.startTransition("${fragment.javaClass.name}_${name}_PAUSE")
        }

        override fun onTransitionResume(transition: Transition) {
            fragment.endTransition("${fragment.javaClass.name}_${name}_PAUSE")
        }
    }
}