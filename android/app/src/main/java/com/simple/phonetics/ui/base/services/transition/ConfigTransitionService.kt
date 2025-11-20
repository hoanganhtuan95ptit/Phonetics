package com.simple.phonetics.ui.base.services.transition

import android.graphics.Color
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.Transition
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import com.simple.coreapp.Param.ROOT_TRANSITION_NAME

interface ConfigTransitionService {

    fun setupTransitionConfig(fragment: Fragment)
}

class ConfigTransitionServiceImpl : ConfigTransitionService {

    private val transitionDuration = 350L
    private var isCanUseTransition: Boolean = false


    override fun setupTransitionConfig(fragment: Fragment) {

        val transitionName = fragment.arguments?.getString(ROOT_TRANSITION_NAME)

        isCanUseTransition = !transitionName.isNullOrBlank()


        setTransitionAnimation(fragment = fragment)


        fragment.viewLifecycleOwnerLiveData.observe(fragment) {

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
}

private class DefaultTransitionListener(val name: String, val fragment: Fragment) : Transition.TransitionListener {
    
    val fragmentName by lazy { 
        
        fragment.javaClass.simpleName
    }

    override fun onTransitionStart(transition: Transition) {
//        Log.d("tuanha", "onTransitionStart: ${fragment.javaClass.simpleName}")
        fragment.startTransition("${fragmentName}_${name}_START")
    }

    override fun onTransitionEnd(transition: Transition) {
//        Log.d("tuanha", "onTransitionEnd: ${fragmentName}")
        fragment.endTransition("${fragmentName}_${name}_START")
    }

    override fun onTransitionCancel(transition: Transition) {
//        Log.d("tuanha", "onTransitionCancel: ${fragmentName}")
        fragment.endTransition("${fragmentName}_${name}_START")
    }

    override fun onTransitionPause(transition: Transition) {
//        Log.d("tuanha", "onTransitionPause: ${fragmentName}")
        fragment.startTransition("${fragmentName}_${name}_PAUSE")
    }

    override fun onTransitionResume(transition: Transition) {
//        Log.d("tuanha", "onTransitionResume: ${fragmentName}")
        fragment.endTransition("${fragmentName}_${name}_PAUSE")
    }
}