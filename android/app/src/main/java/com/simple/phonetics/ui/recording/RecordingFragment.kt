package com.simple.phonetics.ui.recording

import android.Manifest
import android.content.ComponentCallbacks
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.asFlow
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.JustifyContent
import com.permissionx.guolindev.PermissionX
import com.simple.adapter.MultiAdapter
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.ui.view.setMargin
import com.simple.coreapp.ui.view.setPadding
import com.simple.coreapp.ui.view.setSize
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.crashlytics.logCrashlytics
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.event.sendEvent
import com.simple.image.setImage
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.databinding.DialogListBinding
import com.simple.phonetics.databinding.LayoutConfirmRecordingBinding
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.fragments.BaseActionFragment
import com.simple.phonetics.utils.exts.createFlexboxLayoutManager
import com.simple.state.isCompleted
import com.simple.state.isRunning
import com.simple.state.toSuccess

class RecordingFragment : BaseActionFragment<LayoutConfirmRecordingBinding, DialogListBinding, RecordingViewModel>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAction()
        setupRecyclerView()

        observeData()
    }

    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): DialogListBinding {

        return DialogListBinding.inflate(inflater, container, false)
    }

    override fun createBindingAction(): LayoutConfirmRecordingBinding {

        return LayoutConfirmRecordingBinding.inflate(LayoutInflater.from(requireContext()))
    }

    private fun setupAction() {

        val binding = bindingAction ?: return

        binding.root.setDebouncedClickListener {

            PermissionX.init(this.requireActivity()).permissions(REQUIRED_PERMISSIONS_RECORD_AUDIO.toList()).request { allGranted, _, _ ->

                if (allGranted) speak()
            }
        }
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        MultiAdapter().apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null

            val layoutManager = createFlexboxLayoutManager(context = context) {

                logCrashlytics(
                    event = "RECORDING",
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
            val bindingAction = bindingAction ?: return@observe

            binding.vAnchor.delegate.setBackground(Background(backgroundColor = it.colorDivider, cornerRadius = DP.DP_100))

            binding.root.delegate.setBackground(Background(backgroundColor = it.colorBackground, cornerRadius_TL = DP.DP_16, cornerRadius_TR = DP.DP_16))
            bindingAction.root.delegate.setBackground(Background(backgroundColor = it.colorBackground, cornerRadius_TL = DP.DP_16, cornerRadius_TR = DP.DP_16))
        }

        speakState.observe(viewLifecycleOwner) {

            val arguments = arguments ?: return@observe

            val data = it.toSuccess()?.data.orEmpty()

            if (data.isNotEmpty()) {

                sendEvent(arguments.getString(Param.KEY_REQUEST).orEmpty(), data)
                if (isAdded && !isStateSaved) dismiss()
            }
        }

        actionInfo.observe(viewLifecycleOwner) { item ->

            val binding = bindingAction?.frameRecording ?: return@observe

            if (item.anim != null) {
                binding.ivImage.setAnimation(item.anim)
                binding.ivImage.playAnimation()
            }
            if (item.image != null) {
                binding.ivImage.setImage(item.image)
            }

            binding.progressBar.setVisible(item.isLoading)

            binding.root.setSize(item.size)
            binding.root.setMargin(item.margin)
            binding.root.setPadding(item.padding)
            binding.root.delegate.setBackground(item.background)

            binding.ivImage.setSize(item.imageSize)
            binding.ivImage.setMargin(item.imageMargin)
            binding.ivImage.setPadding(item.imagePadding)
            binding.ivImage.delegate.setBackground(item.imageBackground)
        }

        viewItemList.asFlow().launchCollect(viewLifecycleOwner) {

            val binding = binding ?: return@launchCollect

            binding.recyclerView.submitListAwait(it)
        }

        arguments?.getBoolean(Param.REVERSE)?.let {

            updateReverse(it)
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

    companion object {

        private val REQUIRED_PERMISSIONS_RECORD_AUDIO = arrayOf(Manifest.permission.RECORD_AUDIO)
    }
}

@Deeplink
class RecordingDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return DeeplinkManager.RECORDING
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is MainActivity) return false

        val fragment = RecordingFragment()
        fragment.arguments = bundleOf(*extras?.toList().orEmpty().toTypedArray())
        fragment.showOrAwaitDismiss(componentCallbacks.supportFragmentManager, "")

        return true
    }
}