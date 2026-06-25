package com.simple.feature.pronunciation_assessment

import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asFlow
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.simple.autobind.annotation.AutoBind
import com.simple.coreapp.utils.ext.resize
import com.simple.coreapp.utils.extentions.postValue
import com.simple.feature.pronunciation_assessment.databinding.LayoutPronunciationAssessmentBinding
import com.simple.phonetics.ui.speak.SpeakFragment
import com.simple.phonetics.ui.speak.SpeakViewModel
import com.simple.service.FragmentViewCreatedService
import com.simple.state.doSuccess
import com.simple.state.isLoading
import com.simple.state.isSuccess
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.coroutines.resume

@AutoBind(SpeakFragment::class)
class PronunciationService : FragmentViewCreatedService {

    private lateinit var viewModel: PronunciationViewModel

    private lateinit var speakViewModel: SpeakViewModel

    override fun setup(fragment: Fragment) {

        if (fragment !is SpeakFragment) {
            return
        }

        viewModel = fragment.viewModels<PronunciationViewModel>().value
        speakViewModel = fragment.viewModel<SpeakViewModel>().value

        val bindingActionPronunciation = LayoutPronunciationAssessmentBinding.inflate(fragment.layoutInflater, fragment.bindingAction!!.framePronunciation)

        speakViewModel.text.asFlow().launchCollect(fragment.viewLifecycleOwner) {

            bindingActionPronunciation.tvTextToSpeech.text = it
        }

        speakViewModel.phoneticsState.asFlow().launchCollect(fragment.viewLifecycleOwner) {

            it.doSuccess {

                bindingActionPronunciation.tvActionPlay.sentences = it
            }
        }


        viewModel.initState.launchCollect(fragment.viewLifecycleOwner) {

            val binding = fragment.binding ?: return@launchCollect
            val bindingAction = fragment.bindingAction ?: return@launchCollect

            it.doSuccess {

                expan(fragment)
            }

            bindingAction.root.awaitDelayedTransition {

                val visibility = if (it.isSuccess()) {
                    View.GONE
                } else if (it.isLoading()) {
                    View.INVISIBLE
                } else {
                    View.VISIBLE
                }

                bindingAction.frameCopy.root.visibility = visibility
                bindingAction.frameSpeak.root.visibility = visibility
                bindingAction.frameReading.root.visibility = visibility

                bindingActionPronunciation.tvTextToSpeech.isVisible = it.isSuccess()
            }

            if (it.isSuccess()) binding.root.awaitDelayedTransition {

                binding.vAnchor.isVisible = false
                binding.frameHeader.root.isVisible = true
            }
        }

        combine(
            viewModel.recordState,
            viewModel.assessmentState
        ) { record, assessment ->
            record to assessment
        }.launchCollect(fragment.viewLifecycleOwner) { (record, assessment) ->

            val bindingAction = fragment.bindingAction ?: return@launchCollect

            bindingAction.root.awaitDelayedTransition {

                bindingActionPronunciation.tvAudio.isVisible = assessment.isSuccess()
                bindingActionPronunciation.tvTextToSpeech.isVisible = (!record.isLoading() && !assessment.isLoading() && viewModel.initState.value.isSuccess())
            }
        }

        viewModel.assessmentState.launchCollect(fragment.viewLifecycleOwner) {

            it.doSuccess {

                bindingActionPronunciation.tvAudio.audioPath = it.audioFilePath.orEmpty()
                speakViewModel.sentenceScore.postValue(it)
            }
        }
    }

    private suspend fun expan(fragment: SpeakFragment) {

        val view = fragment.view ?: return
        val dialog = fragment.dialog as BottomSheetDialog

        val behavior = dialog.behavior
        val container = dialog.findViewById<View>(R.id.container)!!
        val bottomSheet = dialog.findViewById<View>(R.id.design_bottom_sheet)!!

        val top = bottomSheet.top
        behavior.isDraggable = false
        behavior.skipCollapsed = false
        behavior.peekHeight = container.height

        view.resize(container.width, container.height)
        bottomSheet.resize(container.width, container.height)
        bottomSheet.setPadding(0, 0, 0, 0)
        view.translationY = top + 0f

        view.animate().translationY(0f).awaitEnd()
    }

    private suspend fun ViewPropertyAnimator.awaitEnd() = suspendCancellableCoroutine<Unit> { cont ->

        withEndAction {
            if (cont.isActive) {
                cont.resume(Unit)
            }
        }.start()

        cont.invokeOnCancellation {
            cancel()
        }
    }

    private suspend fun ViewGroup.awaitDelayedTransition(transition: Transition = AutoTransition(), changes: () -> Unit) = suspendCancellableCoroutine<Unit> { cont ->

        val timeoutRunner = Runnable {
            if (cont.isActive) {
                cont.resume(Unit)
            }
        }

        postDelayed(timeoutRunner, 350)

        val listener = object : Transition.TransitionListener {

            override fun onTransitionStart(transition: Transition) {

                removeCallbacks(timeoutRunner)
            }

            override fun onTransitionEnd(transition: Transition) {

                transition.removeListener(this)

                if (cont.isActive) {
                    cont.resume(Unit)
                }
            }

            override fun onTransitionCancel(transition: Transition) {

                transition.removeListener(this)

                if (cont.isActive) {
                    cont.resume(Unit)
                }
            }

            override fun onTransitionPause(transition: Transition) {
            }

            override fun onTransitionResume(transition: Transition) {
            }
        }

        transition.addListener(listener)

        cont.invokeOnCancellation {
            transition.removeListener(listener)
        }

        TransitionManager.beginDelayedTransition(this, transition)

        changes()
    }
}