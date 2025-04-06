package com.simple.phonetics.ui.speak

import android.Manifest
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.core.view.updatePadding
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
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.doOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.crashlytics.logCrashlytics
import com.simple.image.setImage
import com.simple.phonetics.Deeplink
import com.simple.phonetics.Param
import com.simple.phonetics.R
import com.simple.phonetics.databinding.DialogListBinding
import com.simple.phonetics.databinding.LayoutConfirmSpeakBinding
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.adapters.PhoneticsAdapter
import com.simple.phonetics.ui.base.fragments.BaseSheetFragment
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.phonetics.utils.exts.ListPreviewAdapter
import com.simple.phonetics.utils.exts.createFlexboxLayoutManager
import com.simple.phonetics.utils.exts.playMedia
import com.simple.phonetics.utils.exts.playVibrate
import com.simple.phonetics.utils.showAds
import com.simple.state.isCompleted
import com.simple.state.isFailed
import com.simple.state.isRunning
import com.simple.state.toSuccess
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class SpeakFragment : BaseSheetFragment<DialogListBinding, SpeakViewModel>() {


    private val configViewModel: ConfigViewModel by lazy {
        getViewModel(requireActivity(), ConfigViewModel::class)
    }


    private var adapter by autoCleared<MultiAdapter>()

    private var bindingConfirmSpeak by autoCleared<LayoutConfirmSpeakBinding>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindingConfirmSpeak = LayoutConfirmSpeakBinding.inflate(LayoutInflater.from(requireContext()))

        val layoutParam = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParam.gravity = Gravity.BOTTOM

        container?.addView(bindingConfirmSpeak?.root, layoutParam)


        val binding = binding ?: return

        binding.root.doOnChangeHeightStatusAndHeightNavigation(viewLifecycleOwner) { heightStatusBar: Int, heightNavigationBar: Int ->

            this.binding ?: return@doOnChangeHeightStatusAndHeightNavigation
            val bindingConfirmSpeak = bindingConfirmSpeak ?: return@doOnChangeHeightStatusAndHeightNavigation

            binding.root.updatePadding(bottom = heightNavigationBar + DP.DP_24)
            bindingConfirmSpeak.root.updatePadding(bottom = heightNavigationBar + DP.DP_24)
        }

        channelFlow {

            val frameAction = bindingConfirmSpeak?.root ?: return@channelFlow

            val onGlobalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {

                override fun onGlobalLayout() {
                    bindingConfirmSpeak ?: return
                    trySend(frameAction.height)
                }
            }

            frameAction.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)

            awaitClose {
                frameAction.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
            }
        }.distinctUntilChanged().launchCollect(viewLifecycleOwner) {

            binding.recyclerView.updatePadding(bottom = it)
        }


        setupSpeak()
        setupListener()
        setupRecyclerView()

        observeData()
        observeConfigData()

        showAds()
    }

    private fun setupSpeak() {

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

    private fun setupListener() {

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

        adapter = MultiAdapter(phoneticsAdapter, *ListPreviewAdapter()).apply {

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

            binding.root.delegate.setBackground(Background(backgroundColor = it.colorBackground, cornerRadius_TL = DP.DP_16, cornerRadius_TR = DP.DP_16))
            binding.vAnchor.delegate.setBackground(Background(backgroundColor = it.colorDivider, cornerRadius = DP.DP_100))

            bindingConfigSpeak.root.setBackgroundColor(it.colorBackground)
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

@com.tuanha.deeplink.annotation.Deeplink
class SpeakDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return Deeplink.SPEAK
    }

    override suspend fun navigation(activity: ComponentActivity, deepLink: String, extras: Bundle?, sharedElement: Map<String, View>?): Boolean {

        if (activity !is MainActivity) return false

        val fragment = SpeakFragment()
        fragment.arguments = extras
        fragment.showOrAwaitDismiss(activity.supportFragmentManager, "")

        return true
    }
}