package com.simple.phonetics.ui.speak

import android.Manifest
import android.content.ComponentCallbacks
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.JustifyContent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.permissionx.guolindev.PermissionX
import com.simple.adapter.MultiAdapter
import com.simple.core.utils.extentions.orZero
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.ui.view.setMargin
import com.simple.coreapp.ui.view.setPadding
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.doOnHeightStatusAndHeightNavigationChange
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.get
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.crashlytics.logCrashlytics
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.image.setImage
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.R
import com.simple.phonetics.databinding.DialogListBinding
import com.simple.phonetics.databinding.LayoutConfirmSpeakBinding
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.adapters.PhoneticsAdapter
import com.simple.phonetics.ui.base.fragments.BaseSheetFragment
import com.simple.phonetics.utils.exts.createFlexboxLayoutManager
import com.simple.phonetics.utils.exts.listenerOnHeightChange
import com.simple.phonetics.utils.exts.playMedia
import com.simple.phonetics.utils.exts.playVibrate
import com.simple.phonetics.utils.showAds
import com.simple.state.isCompleted
import com.simple.state.isFailed
import com.simple.state.isRunning
import com.simple.state.toSuccess
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class SpeakFragment : BaseSheetFragment<DialogListBinding, SpeakViewModel>() {


    private val configViewModel: ConfigViewModel by lazy {
        getViewModel(requireActivity(), ConfigViewModel::class)
    }


    private var bindingConfirmSpeak by autoCleared<LayoutConfirmSpeakBinding>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindingConfirmSpeak = LayoutConfirmSpeakBinding.inflate(LayoutInflater.from(requireContext()))

        setupAction()
        setupRecyclerView()

        observeData()
        observeConfigData()

        showAds()
    }

    private fun setupAction() {

        val binding = bindingConfirmSpeak ?: return

        val layoutParam = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        container?.addView(binding.root, layoutParam)

        behavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

            override fun onStateChanged(bottomSheet: View, newState: Int) {

            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

                val translateY = (1 + slideOffset) * this@SpeakFragment.bottomSheet?.height.orZero() - viewModel.actionHeight.get()
                if (translateY < 0) binding.root.translationY = translateY.absoluteValue
            }
        })

        binding.root.listenerOnHeightChange().launchCollect(viewLifecycleOwner) {

            viewModel.updateActionHeight(it)
        }

        doOnHeightStatusAndHeightNavigationChange { heightStatusBar: Int, heightNavigationBar: Int ->

            val bindingConfirmSpeak = bindingConfirmSpeak ?: return@doOnHeightStatusAndHeightNavigationChange

            bindingConfirmSpeak.root.updatePadding(bottom = heightNavigationBar + DP.DP_24)
        }

        setupActionSpeak()
        setupActionListen()
    }

    private fun setupActionSpeak() {

        val binding = bindingConfirmSpeak ?: return

        binding.frameSpeak.root.setPadding(
            Padding(padding = DP.DP_16)
        )

        binding.frameSpeak.root.setDebouncedClickListener {

            PermissionX.init(this.requireActivity())
                .permissions(REQUIRED_PERMISSIONS_RECORD_AUDIO.toList())
                .request { allGranted, _, _ ->

                    if (allGranted) speak()
                }
        }
    }

    private fun setupActionListen() {

        val binding = bindingConfirmSpeak ?: return

        binding.frameListen.root.setPadding(
            Padding(padding = DP.DP_12)
        )

        binding.frameListen.ivImage.setMargin(
            Margin(margin = DP.DP_8)
        )

        binding.frameListen.root.setDebouncedClickListener {

            listen()
        }
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        val phoneticsAdapter = PhoneticsAdapter { _, item ->

            if (viewModel.isSupportListen.value == true) {

                listen(text = item.data.text)
            }
        }

        MultiAdapter(phoneticsAdapter).apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null

            val layoutManager = createFlexboxLayoutManager(context = context) {

                logCrashlytics(
                    event = "SPEAK",
                    throwable = it,
                    "VIEW_ITEM_SIZE" to "${viewModel.viewItemList.value?.size}"
                )
            }
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.CENTER
            binding.recyclerView.layoutManager = layoutManager
        }
    }

    private fun observeData() = with(viewModel) {

        theme.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe
            val bindingConfigSpeak = bindingConfirmSpeak ?: return@observe

            val background = Background(
                backgroundColor = it.colorBackground,
                cornerRadius_TL = DP.DP_16,
                cornerRadius_TR = DP.DP_16
            )

            binding.root.delegate.setBackground(background = background)
            bindingConfigSpeak.root.delegate.setBackground(background = background)

            binding.vAnchor.delegate.setBackground(Background(backgroundColor = it.colorDivider, cornerRadius = DP.DP_100))
            bindingConfigSpeak.frameSpeak.root.delegate.setBackground(Background(strokeWidth = DP.DP_2, strokeColor = it.colorPrimary, cornerRadius = DP.DP_16))
        }

        speakInfo.observe(viewLifecycleOwner) {

            val binding = bindingConfirmSpeak?.frameSpeak ?: return@observe

            if (it.anim != null) {
                binding.ivImage.setAnimation(it.anim)
                binding.ivImage.playAnimation()
            }
            if (it.image != null) {
                binding.ivImage.setImage(it.image)
            }

            binding.root.setVisible(it.isShow)
            binding.progressBar.setVisible(it.isLoading)
        }

        listenInfo.observe(viewLifecycleOwner) {

            val binding = bindingConfirmSpeak?.frameListen ?: return@observe

            binding.ivImage.setImage(it.image)

            binding.root.setVisible(it.isShow)
            binding.progressBar.setVisible(it.isLoading)
        }

        resultInfo.observe(viewLifecycleOwner) {

            val binding = bindingConfirmSpeak ?: return@observe

            binding.tvMessage.text = it.result
            binding.tvMessage.setVisible(it.isShow)
            binding.tvMessage.delegate.setBackground(it.background)
        }

        speakState.asFlow().launchCollect(viewLifecycleOwner) {

            if (it.isFailed()) viewLifecycleOwner.lifecycleScope.launch {
                playVibrate()
            }
        }

        isCorrectEvent.asFlow().launchCollect(viewLifecycleOwner) {

            val isCorrect = it.getContentIfNotHandled() ?: return@launchCollect

            if (!isCorrect) viewLifecycleOwner.lifecycleScope.launch {
                playVibrate()
            }

            if (isCorrect) viewLifecycleOwner.lifecycleScope.launch {
                playMedia(R.raw.mp3_answer_correct)
            }
        }

        viewItemList.asFlow().launchCollect(viewLifecycleOwner) {

            val binding = binding ?: return@launchCollect

            binding.recyclerView.submitListAwait(it)
        }

        arguments?.getString(Param.TEXT)?.takeIf {

            it.isNotBlank()
        }?.let {

            this.updateText(it)
        }
    }

    private fun observeConfigData() = with(configViewModel) {

        voiceState.observe(viewLifecycleOwner) {

            viewModel.updateSupportSpeak(it.toSuccess()?.data.orEmpty().isNotEmpty())
        }
    }

    private fun speak() {

        val speakState = viewModel.speakState.value

        if (speakState.isRunning()) {

            viewModel.stopSpeak()
        } else if (speakState == null || speakState.isCompleted()) {

            viewModel.startSpeak()
        }
    }

    private fun listen(text: String? = null) {

        val voiceState = viewModel.listenState.value

        if (voiceState.isRunning()) {

            viewModel.stopListen()
        } else if (voiceState == null || voiceState.isCompleted()) viewModel.startListen(
            text = text,
            voiceId = configViewModel.voiceSelect.value ?: 0,
            voiceSpeed = configViewModel.voiceSpeed.value ?: 1f
        )
    }

    companion object {

        private val REQUIRED_PERMISSIONS_RECORD_AUDIO = arrayOf(Manifest.permission.RECORD_AUDIO)
    }
}

@Deeplink
class SpeakDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return DeeplinkManager.SPEAK
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is MainActivity) return false

        val fragment = SpeakFragment()
        fragment.arguments = bundleOf(*extras?.toList().orEmpty().toTypedArray())
        fragment.showOrAwaitDismiss(componentCallbacks.supportFragmentManager, "")

        return true
    }
}