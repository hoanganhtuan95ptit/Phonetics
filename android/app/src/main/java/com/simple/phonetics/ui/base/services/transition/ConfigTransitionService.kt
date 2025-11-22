package com.simple.phonetics.ui.base.services.transition

import android.graphics.Color
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.transition.Transition
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import com.simple.coreapp.Param.ROOT_TRANSITION_NAME
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

private const val TRANSITION_DURATION = 350L

interface ConfigTransitionService {

    var configTransitionViewModel: ConfigTransitionViewModel

    fun setupTransitionConfig(fragment: Fragment)
}

class ConfigTransitionServiceImpl : ConfigTransitionService {


    override lateinit var configTransitionViewModel: ConfigTransitionViewModel


    private var isCanUseTransition: Boolean = false


    override fun setupTransitionConfig(fragment: Fragment) {

        configTransitionViewModel = fragment.viewModels<ConfigTransitionViewModel>().value


        val transitionName = fragment.arguments?.getString(ROOT_TRANSITION_NAME).toString()
        isCanUseTransition = transitionName.isNotBlank()


        fragment.viewLifecycleOwnerLiveData.observe(fragment) {

            fragment.view?.transitionName = transitionName
        }

        setTransitionAnimation(fragment = fragment)
    }

    private fun setTransitionAnimation(fragment: Fragment) {

        fragment.exitTransition = Hold().apply {

            duration = TRANSITION_DURATION
        }.apply {

            addListener(DefaultTransitionListener(name = "exitTransition", fragment = fragment))
        }

        fragment.reenterTransition = Hold().apply {

            duration = TRANSITION_DURATION
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

            duration = TRANSITION_DURATION

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

            duration = TRANSITION_DURATION

            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH

            setPathMotion(MaterialArcMotion())

            interpolator = FastOutSlowInInterpolator()
            scrimColor = Color.TRANSPARENT
        }.apply {

            addListener(DefaultTransitionListener(name = "sharedElementEnterTransition", fragment = fragment))
        }
    }
}

class ConfigTransitionViewModel : ViewModel() {

    val transitionStatus = MutableLiveData(ConcurrentHashMap<String, Boolean>())

    val transitionStatusValue = transitionStatus.asFlow().map {
        it.toList()
    }.map { list ->
        list.all { it.second }
    }

    init {

        transitionStatusValue.launchIn(viewModelScope)
    }

    fun updateTransitionStatus(tag: String, isEnd: Boolean) {

        val map = transitionStatus.value ?: return

        map[tag] = isEnd

        transitionStatus.value = map
    }

    suspend fun onTransitionStatusEndAwait() {

        transitionStatusValue.filter { it }.first()
    }
}

private class DefaultTransitionListener(val name: String, val fragment: Fragment) : Transition.TransitionListener {
    
    private val fragmentName: String by lazy {
        
        fragment.javaClass.simpleName
    }

    override fun onTransitionStart(transition: Transition) {
//        Log.d("tuanha", "onTransitionStart: ${fragment.javaClass.simpleName}")
        fragment.startTransition("${fragmentName}_${name}_START")
        fragment.updateTransitionStatus(tag = "${fragmentName}_${name}_START", isEnd = false)
    }

    override fun onTransitionEnd(transition: Transition) {
//        Log.d("tuanha", "onTransitionEnd: ${fragmentName}")
        fragment.endTransition("${fragmentName}_${name}_START")
        fragment.updateTransitionStatus(tag = "${fragmentName}_${name}_START", isEnd = true)
    }

    override fun onTransitionCancel(transition: Transition) {
//        Log.d("tuanha", "onTransitionCancel: ${fragmentName}")
        fragment.endTransition("${fragmentName}_${name}_START")
        fragment.updateTransitionStatus(tag = "${fragmentName}_${name}_START", isEnd = true)
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

fun Fragment.updateTransitionStatus(tag: String, isEnd: Boolean) {

    if (this is ConfigTransitionService) configTransitionViewModel.updateTransitionStatus(tag = tag, isEnd = isEnd)
}

suspend fun Fragment.onTransitionStatusEndAwait() {

    if (this is ConfigTransitionService) configTransitionViewModel.onTransitionStatusEndAwait()
}