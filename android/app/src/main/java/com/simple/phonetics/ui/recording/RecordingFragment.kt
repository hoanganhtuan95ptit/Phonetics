package com.simple.phonetics.ui.recording

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.asFlow
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.JustifyContent
import com.permissionx.guolindev.PermissionX
import com.simple.adapter.MultiAdapter
import com.simple.coreapp.ui.base.dialogs.sheet.BaseViewModelSheetFragment
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.Deeplink
import com.simple.phonetics.Param
import com.simple.phonetics.databinding.DialogListBinding
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.adapters.ImageStateAdapter
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.phonetics.utils.exts.ListPreviewAdapter
import com.simple.phonetics.utils.exts.createFlexboxLayoutManager
import com.simple.phonetics.utils.sendEvent
import com.simple.state.isCompleted
import com.simple.state.isRunning
import com.simple.state.toSuccess

class RecordingFragment : BaseViewModelSheetFragment<DialogListBinding, RecordingViewModel>() {

    private var adapter by autoCleared<MultiAdapter>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        observeData()
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        val imageStateAdapter = ImageStateAdapter { view, item ->

            if (item.id == RecordingViewModel.ID.SPEAK) PermissionX.init(this.requireActivity())
                .permissions(REQUIRED_PERMISSIONS_RECORD_AUDIO.toList())
                .request { allGranted, _, _ ->
                    if (allGranted) {
                        speak()
                    }
                }
        }

        adapter = MultiAdapter(imageStateAdapter, *ListPreviewAdapter()).apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null
            binding.recyclerView.setItemViewCacheSize(10)

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

            binding.root.delegate.backgroundColor = it.colorBackground
            binding.root.delegate.setBgSelector()
        }

        speakState.observe(viewLifecycleOwner) {

            val arguments = arguments ?: return@observe

            val data = it.toSuccess()?.data.orEmpty()

            if (data.isNotEmpty()) {

                sendEvent(arguments.getString(Param.KEY_REQUEST).orEmpty(), data)
                dismiss()
            }
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

@com.tuanha.deeplink.annotation.Deeplink
class RecordingDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return Deeplink.RECORDING
    }

    override suspend fun navigation(activity: ComponentActivity, deepLink: String, extras: Bundle?, sharedElement: Map<String, View>?): Boolean {

        if (activity !is MainActivity) return false

        val fragment = RecordingFragment()
        fragment.arguments = extras
        fragment.showOrAwaitDismiss(activity.supportFragmentManager, "")

        return true
    }
}