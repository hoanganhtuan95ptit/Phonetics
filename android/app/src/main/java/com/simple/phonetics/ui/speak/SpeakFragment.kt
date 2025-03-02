package com.simple.phonetics.ui.speak

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.asFlow
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.permissionx.guolindev.PermissionX
import com.simple.adapter.MultiAdapter
import com.simple.coreapp.ui.base.dialogs.sheet.BaseViewModelSheetFragment
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.phonetics.Deeplink
import com.simple.phonetics.Param
import com.simple.phonetics.databinding.DialogListBinding
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.adapters.PhoneticsAdapter
import com.simple.phonetics.ui.base.adapters.ImageStateAdapter
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.phonetics.utils.exts.ListPreviewAdapter
import com.simple.state.isCompleted
import com.simple.state.isRunning
import com.simple.state.toSuccess

class SpeakFragment : BaseViewModelSheetFragment<DialogListBinding, SpeakViewModel>() {

    private val configViewModel: ConfigViewModel by lazy {
        getViewModel(requireActivity(), ConfigViewModel::class)
    }

    private var adapter by autoCleared<MultiAdapter>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        observeData()
        observePhoneticsConfigData()
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        val phoneticsAdapter = PhoneticsAdapter { view, item ->

            if (viewModel.isSupportListen.value == true) {

                listen(text = item.data.text)
            }
        }

        val imageStateAdapter = ImageStateAdapter { view, item ->

            if (item.id == SpeakViewModel.ID.LISTEN) {

                listen()
            } else if (item.id == SpeakViewModel.ID.SPEAK) PermissionX.init(this.requireActivity())
                .permissions(REQUIRED_PERMISSIONS_RECORD_AUDIO.toList())
                .request { allGranted, _, _ ->
                    if (allGranted) {
                        speak()
                    }
                }
        }

        adapter = MultiAdapter(phoneticsAdapter, imageStateAdapter, *ListPreviewAdapter()).apply {

            val layoutManager = FlexboxLayoutManager(context)
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.CENTER

            binding.recyclerView.adapter = this
            binding.recyclerView.layoutManager = layoutManager
        }
    }

    private fun observeData() = with(viewModel) {

        theme.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.root.delegate.backgroundColor = it.colorBackground
            binding.root.delegate.setBgSelector()
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

    private fun observePhoneticsConfigData() = with(configViewModel) {

        voiceState.observe(viewLifecycleOwner) {

            viewModel.updateSupportSpeak(it.toSuccess()?.data.orEmpty().isNotEmpty())
        }

        phoneticSelect.observe(viewLifecycleOwner) {

            viewModel.updatePhoneticSelect(it)
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