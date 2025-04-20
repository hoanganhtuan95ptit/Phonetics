package com.simple.phonetics.ui.home

import android.content.ComponentCallbacks
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.android.flexbox.JustifyContent
import com.simple.adapter.MultiAdapter
import com.simple.coreapp.ui.adapters.texts.ClickTextAdapter
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.doOnHeightStatusChange
import com.simple.coreapp.utils.extentions.isActive
import com.simple.crashlytics.logCrashlytics
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.deeplink.sendDeeplink
import com.simple.image.setImage
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Id
import com.simple.phonetics.Param
import com.simple.phonetics.R
import com.simple.phonetics.databinding.FragmentHomeBinding
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.adapters.PhoneticsAdapter
import com.simple.phonetics.ui.base.fragments.BaseFragment
import com.simple.phonetics.ui.home.view.LanguageHomeView
import com.simple.phonetics.ui.home.view.LanguageHomeViewImpl
import com.simple.phonetics.ui.home.view.PasteHomeView
import com.simple.phonetics.ui.home.view.PasteHomeViewImpl
import com.simple.phonetics.ui.home.view.detect.DetectHomeView
import com.simple.phonetics.ui.home.view.detect.DetectHomeViewImpl
import com.simple.phonetics.ui.home.view.event.EventHomeView
import com.simple.phonetics.ui.home.view.event.EventHomeViewImpl
import com.simple.phonetics.ui.home.view.game.GameHomeView
import com.simple.phonetics.ui.home.view.game.GameHomeViewImpl
import com.simple.phonetics.ui.home.view.history.HistoryHomeView
import com.simple.phonetics.ui.home.view.history.HistoryHomeViewImpl
import com.simple.phonetics.ui.home.view.ipa.IpaHomeView
import com.simple.phonetics.ui.home.view.ipa.IpaHomeViewImpl
import com.simple.phonetics.ui.home.view.microphone.MicrophoneHomeView
import com.simple.phonetics.ui.home.view.microphone.MicrophoneHomeViewImpl
import com.simple.phonetics.ui.home.view.phonetic.PhoneticHomeView
import com.simple.phonetics.ui.home.view.phonetic.PhoneticHomeViewImpl
import com.simple.phonetics.ui.home.view.review.ReviewHomeView
import com.simple.phonetics.ui.home.view.review.ReviewHomeViewImpl
import com.simple.phonetics.utils.exts.collectWithLockTransitionIfCached
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.exts.createFlexboxLayoutManager
import com.simple.phonetics.utils.exts.getCurrentOffset
import com.simple.phonetics.utils.exts.submitListAwaitV2
import kotlin.math.absoluteValue


class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>(),
    IpaHomeView by IpaHomeViewImpl(),
    GameHomeView by GameHomeViewImpl(),
    PasteHomeView by PasteHomeViewImpl(),
    EventHomeView by EventHomeViewImpl(),
    ReviewHomeView by ReviewHomeViewImpl(),
    DetectHomeView by DetectHomeViewImpl(),
    HistoryHomeView by HistoryHomeViewImpl(),
    PhoneticHomeView by PhoneticHomeViewImpl(),
    LanguageHomeView by LanguageHomeViewImpl(),
    MicrophoneHomeView by MicrophoneHomeViewImpl() {

    private val configViewModel: ConfigViewModel by lazy {
        getViewModel(requireActivity(), ConfigViewModel::class)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {

            override fun handleOnBackPressed() {

                val binding = binding ?: return

                if (binding.etText.text.isNotEmpty()) if (binding.appBarLayout.getCurrentOffset().absoluteValue >= binding.appBarLayout.totalScrollRange.absoluteValue / 2) {

                    binding.appBarLayout.setExpanded(true, true)
                } else {

                    binding.etText.setText("")
                } else {

                    activity?.finish()
                }
            }
        })

        setupIpa(this)
        setupGame(this)
        setupPaste(this)
        setupEvent(this)
        setupReview(this)
        setupDetect(this)
        setupHistory(this)
        setupHistory(this)
        setupPhonetic(this)
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

        val clickTextAdapter = ClickTextAdapter { view, item ->

            if (item.id.startsWith(Id.SENTENCE) && item.data is Sentence) sendDeeplink(
                deepLink = DeeplinkManager.SPEAK,
                extras = mapOf(Param.TEXT to (item.data as Sentence).text)
            )
        }

        val phoneticsAdapter = PhoneticsAdapter { _, item ->

            if (viewModel.isSupportSpeak.value == true) {

                sendDeeplink(DeeplinkManager.SPEAK, extras = mapOf(Param.TEXT to item.data.text))
            } else if (viewModel.isSupportListen.value == true) {

                startSpeak(text = item.data.text)
            }
        }

        MultiAdapter(clickTextAdapter, phoneticsAdapter).apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null

            val layoutManager = createFlexboxLayoutManager(context = context) {

                logCrashlytics(
                    event = "PHONETICS",
                    throwable = it,
                    "VIEW_ITEM_SIZE" to "${viewModel.viewItemList.value?.size}"
                )
            }

            layoutManager.justifyContent = JustifyContent.FLEX_START
            binding.recyclerView.layoutManager = layoutManager
            binding.recyclerView.updatePadding(left = DP.DP_12, right = DP.DP_12)
        }
    }

    private fun setupRecyclerViewConfig() {

        val binding = binding ?: return

        val textAdapter = ClickTextAdapter { _, _ ->

            sendDeeplink(DeeplinkManager.CONFIG)
        }

        MultiAdapter(textAdapter).apply {

            binding.recFilter.adapter = this
            binding.recFilter.itemAnimator = null
            binding.recFilter.setItemViewCacheSize(10)

            binding.recFilter.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        }
    }

    private fun observeData() = with(viewModel) {

        val fragment = this@HomeFragment

        imageInfo.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.ivPicture.setImage(it.image, CircleCrop())
            binding.ivPicture.setVisible(it.isShow)
        }

        isShowLoading.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.progress.setVisible(it)
        }

        theme.collectWithLockTransitionUntilData(fragment = fragment, tag = "THEME") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

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

        title.collectWithLockTransitionUntilData(fragment = fragment, tag = "TITLE") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.tvTitle.text = it
        }

        enterInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "ENTER") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.etText.hint = it.hint
            binding.etText.setTextColor(it.textColor)
        }

        clearInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "CLEAR") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.tvClear.text = it.text
            binding.frameClear.setVisible(it.isShow)
            binding.tvClear.delegate.setBackground(it.background)
        }

        listenInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "LISTEN") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.ivRead.setVisible(it.isShowPlay)
            binding.ivStop.setVisible(it.isShowPause)
        }

        reverseInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "REVERSE") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.tvReverse.text = it.text
            binding.frameReverse.setVisible(it.isShow)
            binding.tvReverse.delegate.setBackground(it.background)
        }

        viewItemList.collectWithLockTransitionIfCached(fragment = fragment, tag = "VIEW_ITEM_LIST") { data, isFirst ->

            val binding = binding ?: return@collectWithLockTransitionIfCached

            binding.recyclerView.submitListAwaitV2(viewItemList = data, isFirst = isFirst)

            showEvent()
            showReview()
        }
    }

    private fun observePhoneticsConfigData() = with(configViewModel) {

        val fragment = this@HomeFragment

        listenerEnable.observe(viewLifecycleOwner) {

            viewModel.updateSupportSpeak(it)
        }

        translateEnable.observe(viewLifecycleOwner) {

            viewModel.updateSupportTranslate(it)
        }

        listConfig.collectWithLockTransitionIfCached(fragment = fragment, tag = "CONFIG_VIEW_ITEM_LIST") { data, isFirst ->

            val binding = binding ?: return@collectWithLockTransitionIfCached

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
}

@Deeplink
class PhoneticsDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return DeeplinkManager.PHONETICS
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is MainActivity) return false

        val fragment = HomeFragment()
        fragment.arguments = bundleOf(*extras?.toList().orEmpty().toTypedArray())

        val fragmentTransaction = componentCallbacks.supportFragmentManager
            .beginTransaction()

        sharedElement?.forEach { (t, u) ->

            fragmentTransaction.addSharedElement(u, t)
        }

        if (isActive()) fragmentTransaction
            .replace(R.id.fragment_container, fragment, "")
            .addToBackStack("")
            .commitAllowingStateLoss()

        return true
    }
}