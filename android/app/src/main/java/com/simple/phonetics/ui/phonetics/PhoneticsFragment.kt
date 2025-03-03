package com.simple.phonetics.ui.phonetics

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.simple.adapter.MultiAdapter
import com.simple.coreapp.ui.adapters.texts.ClickTextAdapter
import com.simple.coreapp.ui.base.fragments.transition.TransitionFragment
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.doOnHeightStatusChange
import com.simple.image.setImage
import com.simple.phonetics.Deeplink
import com.simple.phonetics.Id
import com.simple.phonetics.Param
import com.simple.phonetics.R
import com.simple.phonetics.databinding.FragmentPhoneticsBinding
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.adapters.IpaAdapters
import com.simple.phonetics.ui.base.adapters.PhoneticsAdapter
import com.simple.phonetics.ui.phonetics.adapters.HistoryAdapter
import com.simple.phonetics.ui.phonetics.view.LanguageView
import com.simple.phonetics.ui.phonetics.view.LanguageViewImpl
import com.simple.phonetics.ui.phonetics.view.PasteView
import com.simple.phonetics.ui.phonetics.view.PasteViewImpl
import com.simple.phonetics.ui.phonetics.view.detect.DetectView
import com.simple.phonetics.ui.phonetics.view.detect.DetectViewImpl
import com.simple.phonetics.ui.phonetics.view.history.HistoryView
import com.simple.phonetics.ui.phonetics.view.history.HistoryViewImpl
import com.simple.phonetics.ui.phonetics.view.ipa.IpaView
import com.simple.phonetics.ui.phonetics.view.ipa.IpaViewImpl
import com.simple.phonetics.ui.phonetics.view.microphone.MicrophoneView
import com.simple.phonetics.ui.phonetics.view.microphone.MicrophoneViewImpl
import com.simple.phonetics.ui.phonetics.view.review.AppReview
import com.simple.phonetics.ui.phonetics.view.review.AppReviewImpl
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.phonetics.utils.exts.ListPreviewAdapter
import com.simple.phonetics.utils.exts.observeWithTransition
import com.simple.phonetics.utils.exts.observeWithTransitionV2
import com.simple.phonetics.utils.exts.submitListAwaitV2
import com.simple.phonetics.utils.sendDeeplink
import com.simple.state.toSuccess


class PhoneticsFragment : TransitionFragment<FragmentPhoneticsBinding, PhoneticsViewModel>(),
    IpaView by IpaViewImpl(),
    AppReview by AppReviewImpl(),
    PasteView by PasteViewImpl(),
    DetectView by DetectViewImpl(),
    HistoryView by HistoryViewImpl(),
    LanguageView by LanguageViewImpl(),
    MicrophoneView by MicrophoneViewImpl() {

    private val configViewModel: ConfigViewModel by lazy {
        getViewModel(requireActivity(), ConfigViewModel::class)
    }

    private var adapter by autoCleared<MultiAdapter>()

    private var adapterConfig by autoCleared<MultiAdapter>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {

            override fun handleOnBackPressed() {
                activity?.finish()
            }
        })

        setupIpa(this)
        setupPaste(this)
        setupDetect(this)
        setupHistory(this)
        setupAppView(this)
        setupHistory(this)
        setupLanguage(this)
        setupMicrophone(this)

        setupSpeak()
        setupInput()
        setupReverse()
        setupRecyclerView()
        setupRecyclerViewConfig()

        observeData()
        observePhoneticsConfigData()
    }

    private fun setupSpeak() {

        val binding = binding ?: return

        binding.ivRead.setDebouncedClickListener {

            startSpeak(text = binding.etText.text.toString())
        }

        binding.ivStop.setDebouncedClickListener {

            viewModel.stopSpeak()
        }
    }

    private fun setupInput() {

        val binding = binding ?: return

        binding.etText.doAfterTextChanged {

            viewModel.getPhonetics(it.toString())

            binding.etText.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (it.toString().isBlank()) 30f else 16f)
        }

        binding.frameClear.setDebouncedClickListener {

            binding.etText.setText("")
        }

        doOnHeightStatusChange {

            binding.root.updatePadding(top = it)
        }
    }

    private fun setupReverse() {

        val binding = binding ?: return

        binding.frameReverse.setOnClickListener {

            viewModel.switchReverse()
        }
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        val ipaAdapter = IpaAdapters { view, item ->

            val transitionName = view.transitionName ?: item.id

            sendDeeplink(
                deepLink = Deeplink.IPA_DETAIL,
                extras = bundleOf(Param.IPA to item.data, Param.ROOT_TRANSITION_NAME to transitionName),
                sharedElement = mapOf(transitionName to view)
            )
        }

        val historyAdapter = HistoryAdapter { _, item ->

            binding.etText.setText(item.id)
        }

        val clickTextAdapter = ClickTextAdapter { view, item ->

            val transitionName = view.transitionName ?: item.id

            if (item.id.startsWith(Id.SENTENCE) && item.data is Sentence) sendDeeplink(
                deepLink = Deeplink.SPEAK,
                extras = bundleOf(Param.TEXT to (item.data as Sentence).text)
            ) else if (item.id.startsWith(Id.IPA_LIST)) sendDeeplink(
                deepLink = Deeplink.IPA_LIST,
                extras = bundleOf(Param.ROOT_TRANSITION_NAME to transitionName),
                sharedElement = mapOf(transitionName to view)
            )
        }

        val phoneticsAdapter = PhoneticsAdapter { _, item ->

            if (viewModel.isSupportSpeak.value == true) {

                sendDeeplink(Deeplink.SPEAK, extras = bundleOf(Param.TEXT to item.data.text))
            } else if (viewModel.isSupportListen.value == true) {

                startSpeak(text = item.data.text)
            }
        }

        adapter = MultiAdapter(ipaAdapter, historyAdapter, clickTextAdapter, phoneticsAdapter, *ListPreviewAdapter()).apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null

            val layoutManager = FlexboxLayoutManager(context)
            layoutManager.justifyContent = JustifyContent.FLEX_START
            binding.recyclerView.layoutManager = layoutManager
            binding.recyclerView.updatePadding(left = DP.DP_12, right = DP.DP_12)
        }
    }

    private fun setupRecyclerViewConfig() {

        val binding = binding ?: return

        val textAdapter = ClickTextAdapter { _, _ ->

            sendDeeplink(Deeplink.CONFIG)
        }

        adapterConfig = MultiAdapter(textAdapter, *ListPreviewAdapter()).apply {

            binding.recFilter.adapter = this

            binding.recFilter.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        }
    }

    private fun observeData() = with(viewModel) {

        val fragment = this@PhoneticsFragment

        imageInfo.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.ivPicture.setImage(it.image, CircleCrop())
            binding.ivPicture.setVisible(it.isShow)
        }

        isShowLoading.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.progress.setVisible(it)
        }

        theme.observeWithTransition(fragment = fragment, owner = viewLifecycleOwner, tag = TAG.THEME.name) {

            val binding = binding ?: return@observeWithTransition

            binding.ivRead.setColorFilter(it.colorPrimary)
            binding.ivStop.setColorFilter(it.colorPrimary)
            binding.ivPaste.setColorFilter(it.colorPrimary)
            binding.ivCamera.setColorFilter(it.colorPrimary)
            binding.ivGallery.setColorFilter(it.colorPrimary)

            binding.progress.progressTintList = ColorStateList.valueOf(it.colorPrimary)

            binding.root.setBackgroundColor(it.colorBackground)
            binding.frameContent.delegate.backgroundColor = it.colorBackground

            binding.frameRootContent.setBackgroundColor(it.colorBackgroundVariant)
        }

        title.observeWithTransition(fragment = fragment, owner = viewLifecycleOwner, tag = TAG.TITLE.name) {

            val binding = binding ?: return@observeWithTransition

            binding.tvTitle.text = it
        }

        enterInfo.observeWithTransition(fragment = fragment, owner = viewLifecycleOwner, tag = TAG.ENTER.name) {

            val binding = binding ?: return@observeWithTransition

            binding.etText.hint = it.hint
            binding.etText.setTextColor(it.textColor)
        }

        clearInfo.observeWithTransition(fragment = fragment, owner = viewLifecycleOwner, tag = TAG.CLEAR.name) {

            val binding = binding ?: return@observeWithTransition

            binding.tvClear.text = it.text
            binding.frameClear.setVisible(it.isShow)
            binding.tvClear.delegate.setBackground(it.background)
        }

        listenInfo.observeWithTransition(fragment = fragment, owner = viewLifecycleOwner, tag = TAG.LISTEN.name) {

            val binding = binding ?: return@observeWithTransition

            binding.ivRead.setVisible(it.isShowPlay)
            binding.ivStop.setVisible(it.isShowPause)
        }

        reverseInfo.observeWithTransition(fragment = fragment, owner = viewLifecycleOwner, tag = TAG.REVERSE.name) {

            val binding = binding ?: return@observeWithTransition

            binding.tvReverse.text = it.text
            binding.frameReverse.setVisible(it.isShow)
            binding.tvReverse.delegate.setBackground(it.background)
        }

        viewItemList.observeWithTransitionV2(fragment = fragment, owner = viewLifecycleOwner, tag = com.simple.phonetics.TAG.VIEW_ITEM_LIST.name) { data, isFirst ->

            val binding = binding ?: return@observeWithTransitionV2

            binding.recyclerView.submitListAwaitV2(viewItemList = data, isFirst = isFirst)
        }
    }

    private fun observePhoneticsConfigData() = with(configViewModel) {

        val fragment = this@PhoneticsFragment

        voiceState.observe(viewLifecycleOwner) {

            viewModel.updateSupportSpeak(it.toSuccess()?.data.orEmpty().isNotEmpty())
        }

        phoneticSelect.observe(viewLifecycleOwner) {

            viewModel.updatePhoneticSelect(it)
        }

        translateEnable.observe(viewLifecycleOwner) {

            viewModel.updateSupportTranslate(it)
        }

        outputLanguage.observe(viewLifecycleOwner) {

            viewModel.updateOutputLanguage(it)
        }

        listConfig.observeWithTransitionV2(fragment = fragment, owner = viewLifecycleOwner, tag = TAG.CONFIG_VIEW_ITEM_LIST.name) { data, isFirst ->

            val binding = binding ?: return@observeWithTransitionV2

            binding.recFilter.submitListAwaitV2(viewItemList = data, isFirst = isFirst)
        }
    }

    private fun startSpeak(text: String) {

        viewModel.startSpeak(
            text = text,

            voiceId = configViewModel.voiceSelect.value ?: 0,
            voiceSpeed = configViewModel.voiceSpeed.value ?: 1f
        )
    }

    private enum class TAG {
        ENTER, CLEAR, TITLE, THEME, LISTEN, REVERSE, CONFIG_VIEW_ITEM_LIST
    }
}

@com.tuanha.deeplink.annotation.Deeplink
class PhoneticsDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return Deeplink.PHONETICS
    }

    override suspend fun navigation(activity: ComponentActivity, deepLink: String, extras: Bundle?, sharedElement: Map<String, View>?): Boolean {

        if (activity !is MainActivity) return false

        val fragment = PhoneticsFragment()
        fragment.arguments = extras

        val fragmentTransaction = activity.supportFragmentManager
            .beginTransaction()

        sharedElement?.forEach { (t, u) ->

            fragmentTransaction.addSharedElement(u, t)
        }

        fragmentTransaction.replace(R.id.fragment_container, fragment, "")
            .addToBackStack("")
            .commit()

        return true
    }
}