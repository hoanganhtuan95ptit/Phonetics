package com.simple.phonetics.ui.speak.services.pronunciation_assessment

import android.graphics.Color
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.permissionx.guolindev.PermissionX
import com.simple.autobind.annotation.AutoBind
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.ext.resize
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setInvisible
import com.simple.coreapp.utils.ext.setText
import com.simple.coreapp.utils.ext.setVisible
import com.simple.image.setImage
import com.simple.phonetics.ui.speak.SpeakFragment
import com.simple.phonetics.ui.speak.SpeakFragment.Companion.REQUIRED_PERMISSIONS_RECORD_AUDIO
import com.simple.phonetics.ui.speak.SpeakViewModel
import com.simple.phonetics.ui.speak.services.SpeakService
import com.simple.phonetics.utils.AppNew
import com.simple.phonetics.utils.exts.value
import com.simple.state.isCompleted
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AutoBind(SpeakFragment::class)
class PronunciationAssessmentService : SpeakService {

    private lateinit var viewModel: PronunciationAssessmentViewModel
    private lateinit var speakViewModel: SpeakViewModel

    override fun setup(fragment: SpeakFragment) {

        viewModel = fragment.viewModels<PronunciationAssessmentViewModel>().value
        speakViewModel = fragment.getViewModel(fragment, SpeakViewModel::class)

        val binding = fragment.binding
        val bindingAction = fragment.bindingAction

        bindingAction?.tvPronunciation?.setVisible(true)
        bindingAction?.tvPronunciation?.setDebouncedClickListener {

            val view = fragment.view ?: return@setDebouncedClickListener
            val dialog = fragment.dialog as BottomSheetDialog

            val behavior = dialog.behavior
            val container = dialog.findViewById<View>(com.google.android.material.R.id.container)!!
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)!!


            val top = bottomSheet.top
            behavior.isDraggable = true
            behavior.skipCollapsed = false
            behavior.peekHeight = container.height
            view.resize(container.width, container.height)
            view.translationY = top + 0f

            fragment.viewLifecycleOwner.lifecycleScope.launch {

                channelFlow {

                    view.animate().translationY(0f).withEndAction {
                        trySend(Unit)
                    }.start()

                    awaitClose()
                }.first()

                channelFlow {

                    val transitionListener = object : Transition.TransitionListener {
                        override fun onTransitionStart(transition: Transition) {
                        }

                        override fun onTransitionEnd(transition: Transition) {
                            trySend(Unit)
                        }

                        override fun onTransitionCancel(transition: Transition) {
                        }

                        override fun onTransitionPause(transition: Transition) {
                        }

                        override fun onTransitionResume(transition: Transition) {
                        }

                    }

                    val transition = AutoTransition()
                    transition.addListener(transitionListener)

                    TransitionManager.beginDelayedTransition(bindingAction.root, transition)
                    bindingAction.tvPronunciation.setVisible(false)

                    awaitClose {

                        transition.removeListener(transitionListener)
                    }
                }.first()

                if (AppNew.isNew("PronunciationAssessmentNew", 10)) Balloon.Builder(fragment.requireContext())
                    .setWidth(BalloonSizeSpec.WRAP)
                    .setHeight(BalloonSizeSpec.WRAP)
                    .setText("Vui lòng phát âm lại")
                    .setTextColor(Color.WHITE)
                    .setTextSize(15f)
                    .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                    .setArrowSize(10)
                    .setArrowPosition(0.5f)
                    .setPadding(12)
                    .setCornerRadius(8f)
                    .setBalloonAnimation(BalloonAnimation.ELASTIC)
                    .setLifecycleOwner(fragment.viewLifecycleOwner)
                    .build().showAlignTop(bindingAction.frameSpeak.root)

            }

            bindingAction.frameSpeak.root.setDebouncedClickListener {

                PermissionX.init(fragment.requireActivity())
                    .permissions(REQUIRED_PERMISSIONS_RECORD_AUDIO.toList())
                    .request { allGranted, _, _ ->

                        if (allGranted) speak()
                    }
            }
        }

        viewModel.speakInfo.launchCollect(fragment.viewLifecycleOwner) {

            val binding = bindingAction?.frameSpeak ?: return@launchCollect

            if (it.anim != null) {
                binding.ivImage.setAnimation(it.anim)
                binding.ivImage.playAnimation()
            }
            if (it.image != null) {
                binding.ivImage.setImage(it.image)
            }

            binding.root.isClickable = it.isShow
            binding.root.setInvisible(!it.isShow)
            binding.progressBar.setVisible(it.isLoading)
        }

        viewModel.resultInfo.launchCollect(fragment.viewLifecycleOwner) {

            val binding = bindingAction ?: return@launchCollect

            binding.tvMessage.setText(it.result)
            binding.tvMessage.setVisible(it.isShow)
            binding.tvMessage.setBackground(it.background)
        }

        viewModel.viewItemList.launchCollect(fragment.viewLifecycleOwner) {

            speakViewModel.otherViewItemList.value = it
        }

        viewModel.buttonInfo.launchCollect(fragment.viewLifecycleOwner) {

            bindingAction?.tvPronunciation?.setText(it.text)
            bindingAction?.tvPronunciation?.setVisible(it.isShow)
            bindingAction?.tvPronunciation?.setBackground(it.background)
        }

        speakViewModel.text.observe(fragment.viewLifecycleOwner) {

            viewModel.updateText(it)
        }
    }

    private fun speak() {

        val speakState = viewModel.assessmentState.value

        if (speakState == null || speakState.isCompleted()) {

            viewModel.startSpeak()
        } else if (!speakState.isCompleted()) {

            viewModel.stopSpeak()
        }
    }
}