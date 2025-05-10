package com.simple.phonetics.ui.speak

import android.Manifest
import android.content.ComponentCallbacks
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.JustifyContent
import com.permissionx.guolindev.PermissionX
import com.simple.adapter.MultiAdapter
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.Margin
import com.simple.coreapp.ui.view.Padding
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.ui.view.setMargin
import com.simple.coreapp.ui.view.setPadding
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.image.setImage
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.R
import com.simple.phonetics.databinding.DialogListBinding
import com.simple.phonetics.databinding.LayoutActionConfirmSpeakBinding
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.adapters.PhoneticsAdapter
import com.simple.phonetics.ui.base.fragments.BaseActionFragment
import com.simple.phonetics.utils.exts.createFlexboxLayoutManager
import com.simple.phonetics.utils.exts.playMedia
import com.simple.phonetics.utils.exts.playVibrate
import com.simple.phonetics.utils.showAds
import com.simple.state.isCompleted
import com.simple.state.isFailed
import com.simple.state.isRunning
import kotlinx.coroutines.launch

class SpeakFragment : BaseActionFragment<LayoutActionConfirmSpeakBinding, DialogListBinding, SpeakViewModel>() {

    private val configViewModel: ConfigViewModel by lazy {
        getViewModel(requireActivity(), ConfigViewModel::class)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupActionSpeak()
        setupActionListen()
        setupRecyclerView()

        observeData()
    }

    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): DialogListBinding {

        return DialogListBinding.inflate(inflater, container, false)
    }

    override fun createBindingAction(): LayoutActionConfirmSpeakBinding {

        return LayoutActionConfirmSpeakBinding.inflate(LayoutInflater.from(requireContext()))
    }

    override fun onDestroy() {
        super.onDestroy()

        showAds()
    }

    private fun setupActionSpeak() {

        val binding = bindingAction ?: return

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

        val binding = bindingAction ?: return

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

            val layoutManager = createFlexboxLayoutManager(context = context)
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.CENTER
            binding.recyclerView.layoutManager = layoutManager
        }
    }

    private fun observeData() = with(viewModel) {

        theme.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe
            val bindingConfigSpeak = bindingAction ?: return@observe

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

            val binding = bindingAction?.frameSpeak ?: return@observe

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

            val binding = bindingAction?.frameListen ?: return@observe

            binding.ivImage.setImage(it.image)

            binding.root.setVisible(it.isShow)
            binding.progressBar.setVisible(it.isLoading)
        }

        resultInfo.observe(viewLifecycleOwner) {

            val binding = bindingAction ?: return@observe

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

    private fun speak() {

        val speakState = viewModel.speakState.value

        if (speakState.isRunning()) {

            viewModel.stopSpeak()
        } else if (speakState == null || speakState.isCompleted()) {

            viewModel.startSpeak()
        }
    }

    private fun listen(text: String? = null) {

        val voiceState = viewModel.readingState.value

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