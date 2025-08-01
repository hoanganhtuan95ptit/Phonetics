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
import androidx.lifecycle.asFlow
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.JustifyContent
import com.simple.adapter.MultiAdapter
import com.simple.core.utils.extentions.asObject
import com.simple.coreapp.ui.adapters.texts.ClickTextAdapter
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setText
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.doOnHeightStatusChange
import com.simple.coreapp.utils.extentions.isActive
import com.simple.crashlytics.logCrashlytics
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.deeplink.sendDeeplink
import com.simple.event.listenerEvent
import com.simple.event.sendEvent
import com.simple.image.setImage
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.EventName
import com.simple.phonetics.Id
import com.simple.phonetics.Param
import com.simple.phonetics.R
import com.simple.phonetics.databinding.FragmentHomeBinding
import com.simple.phonetics.entities.Sentence
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.adapters.PhoneticsAdapter
import com.simple.phonetics.ui.base.fragments.BaseFragment
import com.simple.phonetics.ui.home.adapters.SentenceViewItem
import com.simple.phonetics.ui.home.view.HomeView
import com.simple.phonetics.ui.view.popup.PopupView
import com.simple.phonetics.ui.view.popup.PopupViewModel
import com.simple.phonetics.utils.exts.collectWithLockTransitionIfCached
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.exts.createFlexboxLayoutManager
import com.simple.phonetics.utils.exts.getCurrentOffset
import com.simple.phonetics.utils.exts.getOrTransparent
import com.simple.phonetics.utils.exts.submitListAwaitV2
import com.simple.state.toSuccess
import java.util.ServiceLoader
import kotlin.math.absoluteValue

class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {

    private val popupViewModel: PopupViewModel by lazy {
        getViewModel(requireActivity(), PopupViewModel::class)
    }

    private val configViewModel: ConfigViewModel by lazy {
        getViewModel(requireActivity(), ConfigViewModel::class)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {

            override fun handleOnBackPressed() {

                val binding = binding ?: return

                if (binding.etText.text.toString().isNotEmpty()) if (binding.appBarLayout.getCurrentOffset().absoluteValue >= binding.appBarLayout.totalScrollRange.absoluteValue / 2) {

                    binding.appBarLayout.setExpanded(true, true)
                } else {

                    binding.etText.setText("")
                } else {

                    activity?.finish()
                }
            }
        })

        ServiceLoader.load(HomeView::class.java).toList().forEach { it.setup(this) }
        ServiceLoader.load(PopupView::class.java).sortedBy { it.priority() }.forEach { it.setup(this) }

        setupSpeak()
        setupInput()
        setupReverse()
        setupRecyclerView()
        setupRecyclerViewConfig()

        observeData()
        observePopupData()
        observePhoneticsConfigData()
    }

    private fun setupSpeak() {

        val binding = binding ?: return

        binding.ivRead.setDebouncedClickListener {

            startSpeak(text = binding.etText.text.toString())
        }

        binding.ivStop.setDebouncedClickListener {

            viewModel.stopReading()
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

        val clickTextAdapter = ClickTextAdapter { _, item ->

            if (item.id.startsWith(Id.SENTENCE) && item.data is Sentence) sendDeeplink(
                deepLink = DeeplinkManager.SPEAK,
                extras = mapOf(Param.TEXT to (item.data as Sentence).text)
            )
        }

        val phoneticsAdapter = PhoneticsAdapter { _, item ->

            if (viewModel.isSupportSpeak.value == true) {

                sendDeeplink(DeeplinkManager.SPEAK, extras = mapOf(Param.TEXT to item.data.text))
            } else if (viewModel.isSupportReading.value == true) {

                startSpeak(text = item.data.text)
            }
        }

        listenerEvent(viewLifecycleOwner.lifecycle, EventName.SENTENCE_VIEW_ITEM_CLICKED) {

            val (_, item) = it.asObject<Pair<View, SentenceViewItem>>()

            sendDeeplink(DeeplinkManager.SPEAK, extras = mapOf(Param.TEXT to item.data.text))
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

            layoutManager.alignItems = AlignItems.FLEX_START

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

            binding.ivRead.setColorFilter(it.getOrTransparent("colorPrimary"))
            binding.ivStop.setColorFilter(it.getOrTransparent("colorPrimary"))
            binding.ivPaste.setColorFilter(it.getOrTransparent("colorPrimary"))
            binding.ivCamera.setColorFilter(it.getOrTransparent("colorPrimary"))
            binding.ivGallery.setColorFilter(it.getOrTransparent("colorPrimary"))

            binding.progress.progressTintList = ColorStateList.valueOf(it.getOrTransparent("colorPrimary"))

            binding.root.setBackgroundColor(it.getOrTransparent("colorBackground"))
            binding.frameContent.setBackground(Background(backgroundColor = it.getOrTransparent("colorBackground"), cornerRadius_BL = DP.DP_16, cornerRadius_BR = DP.DP_16))

            binding.vTemp.setBackgroundColor(it.getOrTransparent("colorBackgroundVariant"))
            binding.frameRootContent.setBackgroundColor(it.getOrTransparent("colorBackgroundVariant"))
        }

        title.collectWithLockTransitionUntilData(fragment = fragment, tag = "TITLE") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.tvTitle.setText(it)
        }

        enterInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "ENTER") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.etText.hint = it.hint.textChar
            binding.etText.setTextColor(it.textColor)
        }

        clearInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "CLEAR") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.tvClear.setText(it.text)
            binding.frameClear.setVisible(it.isShow)
            binding.tvClear.setBackground(it.background)
        }

        readingInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "LISTEN") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.ivRead.setVisible(it.isShowPlay)
            binding.ivStop.setVisible(it.isShowPause)
        }

        reverseInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "REVERSE") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.tvReverse.setText(it.text)
            binding.frameReverse.setVisible(it.isShow)
            binding.tvReverse.setBackground(it.background)
        }

        viewItemList.collectWithLockTransitionIfCached(fragment = fragment, tag = "VIEW_ITEM_LIST") { data, isFirst ->

            val binding = binding ?: return@collectWithLockTransitionIfCached

            binding.recyclerView.submitListAwaitV2(viewItemList = data, isFirst = isFirst)

            sendEvent(EventName.SHOW_POPUP, bundleOf())
        }
    }

    private fun observePopupData() = with(popupViewModel) {

        popupEvent.asFlow().launchCollect(viewLifecycleOwner) { event ->

            val map = event.getContentIfNotHandled() ?: return@launchCollect

            map.values.mapNotNull {

                it.toSuccess()?.data
            }.forEach {

                sendDeeplink(deepLink = it.deepLink, extras = it.extras)
            }
        }
    }

    private fun observePhoneticsConfigData() = with(configViewModel) {

        val fragment = this@HomeFragment

        translateEnable.observe(viewLifecycleOwner) {

            viewModel.updateSupportTranslate(it)
        }

        listConfig.collectWithLockTransitionIfCached(fragment = fragment, tag = "CONFIG_VIEW_ITEM_LIST") { data, isFirst ->

            val binding = binding ?: return@collectWithLockTransitionIfCached

            binding.recFilter.submitListAwaitV2(viewItemList = data, isFirst = isFirst)
        }
    }

    private fun startSpeak(text: String) {

        viewModel.startReading(
            text = text
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