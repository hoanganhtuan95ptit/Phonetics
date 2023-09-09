package com.simple.phonetics.ui.phonetics

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.TypedValue
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionSet
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.simple.adapter.MultiAdapter
import com.simple.analytics.logAnalytics
import com.simple.coreapp.ui.adapters.SpaceAdapter
import com.simple.coreapp.ui.base.fragments.BaseViewModelFragment
import com.simple.coreapp.utils.FileUtils
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.extentions.beginTransitionAwait
import com.simple.coreapp.utils.extentions.clear
import com.simple.coreapp.utils.extentions.doOnHeightStatusChange
import com.simple.coreapp.utils.extentions.getViewModel
import com.simple.coreapp.utils.extentions.haveText
import com.simple.coreapp.utils.extentions.launchTakeImageFromCamera
import com.simple.coreapp.utils.extentions.launchTakeImageFromGallery
import com.simple.coreapp.utils.extentions.observeLaunch
import com.simple.coreapp.utils.extentions.observeQueue
import com.simple.coreapp.utils.extentions.setDebouncedClickListener
import com.simple.coreapp.utils.extentions.setImage
import com.simple.coreapp.utils.extentions.setTextWhenDiff
import com.simple.coreapp.utils.extentions.setVisible
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.coreapp.utils.extentions.text
import com.simple.state.doRunning
import com.simple.state.isRunning
import com.permissionx.guolindev.PermissionX
import com.simple.coreapp.utils.extentions.doOnHeightStatusAndHeightNavigationChange
import com.simple.phonetics.databinding.FragmentPhoneticsBinding
import com.simple.phonetics.ui.MainViewModel
import com.simple.phonetics.ui.adapters.TextOptionAdapter
import com.simple.phonetics.ui.adapters.TitleAdapter
import com.simple.phonetics.ui.phonetics.adapters.PhoneticsAdapter
import com.simple.phonetics.ui.phonetics.adapters.PhoneticsHistoryAdapter
import com.simple.phonetics.ui.phonetics.adapters.SentenceAdapter
import com.simple.phonetics.ui.phonetics.config.PhoneticsConfigFragment
import com.simple.phonetics.ui.phonetics.config.PhoneticsConfigViewModel
import java.util.Locale


class PhoneticsFragment : BaseViewModelFragment<FragmentPhoneticsBinding, PhoneticsViewModel>() {


    private val takeImageFromCameraResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        val currentPhotoPath = currentPhotoPath ?: return@registerForActivityResult

        if (result.resultCode == RESULT_OK) {

            viewModel.getTextFromImage(currentPhotoPath)

            logAnalytics("TAKE_IMAGE_FROM_CAMERA_SUCCESS" to "TAKE_IMAGE_FROM_CAMERA")
        } else {

            logAnalytics("TAKE_IMAGE_FROM_CAMERA_FAILED" to "TAKE_IMAGE_FROM_CAMERA")
        }
    }

    private val takeImageFromGalleryResult = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->

        uri?.let {

            FileUtils.uriToImageFile(requireContext(), it)
        }?.let {

            viewModel.getTextFromImage(it.absolutePath)

            logAnalytics("TAKE_IMAGE_FROM_GALLERY_SUCCESS" to "TAKE_IMAGE_FROM_GALLERY")
        } ?: let {

            logAnalytics("TAKE_IMAGE_FROM_GALLERY_FAILED" to "TAKE_IMAGE_FROM_GALLERY")
        }
    }


    private val mainViewModel: MainViewModel by lazy {
        getViewModel(requireActivity(), MainViewModel::class)
    }

    private val phoneticsConfigViewModel: PhoneticsConfigViewModel by lazy {
        getViewModel(this, PhoneticsConfigViewModel::class)
    }


    private var currentPhotoPath: String? = null


    private var speak: TextToSpeech? = null

    private var adapter by autoCleared<MultiAdapter>()

    private var clipboard by autoCleared<ClipboardManager>()

    private var adapterConfig by autoCleared<MultiAdapter>()

    private var onPrimaryClipChangedListener by autoCleared<ClipboardManager.OnPrimaryClipChangedListener>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        onPrimaryClipChangedListener = ClipboardManager.OnPrimaryClipChangedListener {

            val binding = binding ?: return@OnPrimaryClipChangedListener

            binding.ivPaste.setVisible(clipboard?.haveText() == true)
        }

        setupMore()
        setupSpeak()
        setupPaste()
        setupInput()
        setupRecyclerView()
        setupRecyclerViewConfig()

        observeData()
        observeMainData()
        observePhoneticsConfigData()
    }

    override fun onResume() {
        super.onResume()

        val binding = binding ?: return

        binding.ivPaste.post {

            binding.ivPaste.setVisible(clipboard?.haveText() == true)
        }

        clipboard?.addPrimaryClipChangedListener(onPrimaryClipChangedListener)
    }

    override fun onPause() {
        super.onPause()

        speak?.stopSpeak()

        clipboard?.removePrimaryClipChangedListener(onPrimaryClipChangedListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        speak?.shutdown()
    }

    private fun setupMore() {

        val binding = binding ?: return

        binding.ivMore.setDebouncedClickListener {

            PhoneticsConfigFragment().show(childFragmentManager, "")
        }
    }

    private fun setupSpeak() {

        val binding = binding ?: return

        speak = TextToSpeech(requireContext()) { status ->

            viewModel.updateSpeakState(status != TextToSpeech.ERROR)

            if (status != TextToSpeech.ERROR) {

                speak?.language = Locale.US

                speak?.voices?.filter { it.locale == Locale.US }?.apply {

                    phoneticsConfigViewModel.updateVoice(this)
                }
            }
        }

        speak?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {

            override fun onStart(p0: String?) {

                viewModel.updateSpeakStatus(true)
            }

            override fun onDone(p0: String?) {

                viewModel.updateSpeakStatus(false)
            }

            @Deprecated("Deprecated in Java", ReplaceWith("viewModel.updateSpeakStatus(false)"))
            override fun onError(p0: String?) {

                viewModel.updateSpeakStatus(false)
            }
        })

        binding.ivRead.setDebouncedClickListener {

            val speak = speak ?: return@setDebouncedClickListener

            speak.speak(binding.etText.text.toString(), TextToSpeech.QUEUE_FLUSH, null, "1")

            logAnalytics("READ_ACTION" to "READ")
        }

        binding.ivStop.setDebouncedClickListener {

            val speak = speak ?: return@setDebouncedClickListener

            speak.stopSpeak()

            logAnalytics("STOP_ACTION" to "STOP")
        }
    }

    private fun setupPaste() {

        val binding = binding ?: return

        binding.ivPaste.setOnClickListener {

            binding.etText.setText(clipboard?.text() ?: "")

            clipboard?.clear()

            logAnalytics("PASTE_ACTION" to "PASTE")
        }

        binding.ivGallery.setDebouncedClickListener {

            PermissionX.init(requireActivity())
                .permissions(REQUIRED_PERMISSIONS_READ_FILE.toList())
                .request { allGranted, _, _ ->
                    if (allGranted) {
                        takeImageFromGalleryResult.launchTakeImageFromGallery()
                        logAnalytics("GALLERY_OPEN_WITH_PERMISSION" to "GALLERY")
                    }
                }

            logAnalytics("GALLERY_OPEN" to "GALLERY")
        }

        binding.ivCamera.setDebouncedClickListener {

            PermissionX.init(requireActivity())
                .permissions(REQUIRED_PERMISSIONS_CAMERA.toList())
                .request { allGranted, _, _ ->
                    if (allGranted) {
                        currentPhotoPath = takeImageFromCameraResult.launchTakeImageFromCamera(requireContext(), "image")?.absolutePath ?: return@request
                        logAnalytics("CAMERA_OPEN_WITH_PERMISSION" to "CAMERA")
                    }
                }

            logAnalytics("CAMERA_OPEN" to "CAMERA")
        }
    }

    private fun setupInput() {

        val binding = binding ?: return

        binding.etText.doAfterTextChanged {

            speak?.stopSpeak()

            viewModel.getPhonetics(it.toString())

            binding.etText.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (it.toString().isBlank()) 30f else 16f)
        }

        binding.tvClear.setDebouncedClickListener {

            binding.etText.setText("")

            logAnalytics("CLEAR_ACTION" to "CLEAR")
        }

        doOnHeightStatusChange {

            binding.root.updatePadding(top = it)
        }
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        val phoneticsAdapter = PhoneticsAdapter { _, phoneticsViewItem ->

            val text = phoneticsViewItem.data.text

            val speak = speak ?: return@PhoneticsAdapter

            speak.speak(text, TextToSpeech.QUEUE_FLUSH, null, "1")

            logAnalytics("PHONETICS" to "CLICK")
        }

        val phoneticsHistoryAdapter = PhoneticsHistoryAdapter { _, item ->

            binding.etText.setText(item.id)

            logAnalytics("PHONETICS_HISTORY" to "CLICK")
        }

        adapter = MultiAdapter(phoneticsAdapter, phoneticsHistoryAdapter, SentenceAdapter(), TitleAdapter(), SpaceAdapter()).apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null

            val layoutManager = FlexboxLayoutManager(context)
            layoutManager.justifyContent = JustifyContent.FLEX_START
            binding.recyclerView.layoutManager = layoutManager
        }
    }

    private fun setupRecyclerViewConfig() {

        val binding = binding ?: return

        val textOptionAdapter = TextOptionAdapter { _, item ->

            PhoneticsConfigFragment().show(childFragmentManager, "")

            logAnalytics("TEXT_OPTION" to item.id)
        }

        adapterConfig = MultiAdapter(textOptionAdapter, SpaceAdapter()).apply {

            binding.recFilter.adapter = this

            binding.recFilter.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        }
    }

    private fun observeData() = with(viewModel) {

        text.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            if (binding.etText.isFocused) return@observe

            binding.etText.setTextWhenDiff(it)
        }

        listViewItemDisplayEvent.observeQueue(viewLifecycleOwner) { event ->

            val binding = binding ?: return@observeQueue

            val anim = !event.hasBeenHandled

            val data = event.getContentIfNotHandled() ?: event.peekContent()

            binding.recyclerView.submitListAwait(data.listViewItem)

            binding.nestedScrollView.setVisible(data.listViewItem.isEmpty())

            data.detectState.doRunning {

                binding.ivPicture.setImage(it, CircleCrop())
            }

            binding.progress.setVisible(data.detectState.isRunning())
            binding.ivPicture.setVisible(data.detectState.isRunning())


            binding.ivStop.setVisible(data.isShowSpeakStatus == true && data.isSpeakStatus == true)
            binding.ivRead.setVisible(data.isShowSpeakStatus == true && data.isSpeakStatus != true)


            binding.tvClear.setVisible(data.isShowClearText == true)


            if (!anim) {

                return@observeQueue
            }

            val transition = TransitionSet().addTransition(ChangeBounds().setDuration(350)).addTransition(Fade().setDuration(350))

            binding.recyclerView.beginTransitionAwait(transition)
        }
    }

    private fun observeMainData() = with(mainViewModel) {

        translateState.observeLaunch(viewLifecycleOwner) {

            phoneticsConfigViewModel.updateTranslateState(it)
        }
    }

    private fun observePhoneticsConfigData() = with(phoneticsConfigViewModel) {

        listConfig.observe(viewLifecycleOwner) {

            adapterConfig?.submitList(it)
        }

        phoneticSelect.observe(viewLifecycleOwner) {

            viewModel.updatePhoneticSelect(it)
        }

        listVoiceViewItem.observe(viewLifecycleOwner) { list ->

            val voiceSelected = list.find { it.isSelect }?.data ?: return@observe

            speak?.voice = voiceSelected
        }

        listVoiceSpeedViewItem.observe(viewLifecycleOwner) { list ->

            val item = list.firstOrNull() ?: return@observe

            speak?.setSpeechRate(item.current)
        }

        listTranslationViewItem.observe(viewLifecycleOwner) { list ->

            viewModel.updateTranslateStatus(list.any { it.isSelect })
        }
    }


    private fun TextToSpeech.stopSpeak() {

        viewModel.updateSpeakStatus(false)

        stop()
    }

    companion object {

        private val REQUIRED_PERMISSIONS_CAMERA = arrayOf(Manifest.permission.CAMERA)

        private val REQUIRED_PERMISSIONS_READ_FILE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}