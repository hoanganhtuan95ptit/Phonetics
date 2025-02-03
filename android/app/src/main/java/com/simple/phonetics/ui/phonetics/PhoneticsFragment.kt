package com.simple.phonetics.ui.phonetics

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.asFlow
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionSet
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.simple.adapter.MultiAdapter
import com.simple.coreapp.ui.adapters.EmptyAdapter
import com.simple.coreapp.ui.adapters.texts.ClickTextAdapter
import com.simple.coreapp.ui.adapters.texts.NoneTextAdapter
import com.simple.coreapp.ui.base.fragments.transition.TransitionFragment
import com.simple.coreapp.ui.view.round.setBackground
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.beginTransitionAwait
import com.simple.coreapp.utils.extentions.doOnHeightStatusChange
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.image.setImage
import com.simple.phonetics.Deeplink
import com.simple.phonetics.R
import com.simple.phonetics.databinding.FragmentPhoneticsBinding
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.config.PhoneticsConfigFragment
import com.simple.phonetics.ui.phonetics.adapters.HistoryAdapter
import com.simple.phonetics.ui.phonetics.adapters.PhoneticsAdapter
import com.simple.phonetics.ui.phonetics.adapters.SentenceAdapter
import com.simple.phonetics.ui.phonetics.view.AppReview
import com.simple.phonetics.ui.phonetics.view.AppReviewImpl
import com.simple.phonetics.ui.phonetics.view.ImageView
import com.simple.phonetics.ui.phonetics.view.ImageViewImpl
import com.simple.phonetics.ui.phonetics.view.LanguageView
import com.simple.phonetics.ui.phonetics.view.LanguageViewImpl
import com.simple.phonetics.ui.phonetics.view.PasteView
import com.simple.phonetics.ui.phonetics.view.PasteViewImpl
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.state.toSuccess


class PhoneticsFragment : TransitionFragment<FragmentPhoneticsBinding, PhoneticsViewModel>(),
    AppReview by AppReviewImpl(),
    PasteView by PasteViewImpl(),
    ImageView by ImageViewImpl(),
    LanguageView by LanguageViewImpl() {

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

        setupPaste(this)
        setupImage(this)
        setupAppView(this)
        setupLanguage(this)

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

        }

        val phoneticsAdapter = PhoneticsAdapter { _, phoneticsViewItem ->

            startSpeak(text = phoneticsViewItem.data.text)
        }

        val historyAdapter = HistoryAdapter { _, item ->

            binding.etText.setText(item.id)
        }

        adapter = MultiAdapter(clickTextAdapter, phoneticsAdapter, historyAdapter, EmptyAdapter(), SentenceAdapter(), NoneTextAdapter()).apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null

            val layoutManager = FlexboxLayoutManager(context)
            layoutManager.justifyContent = JustifyContent.FLEX_START
            binding.recyclerView.layoutManager = layoutManager
        }
    }

    private fun setupRecyclerViewConfig() {

        val binding = binding ?: return

        val textAdapter = ClickTextAdapter { view, item ->

            PhoneticsConfigFragment().show(childFragmentManager, "")
        }

        adapterConfig = MultiAdapter(textAdapter).apply {

            binding.recFilter.adapter = this

            binding.recFilter.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        }
    }

    private fun observeData() = with(viewModel) {

        lockTransition(TAG.THEME.name, TAG.TITLE.name, TAG.ENTER.name, TAG.CLEAR.name, TAG.REVERSE.name)

        theme.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.ivRead.setColorFilter(it.colorPrimary)
            binding.ivStop.setColorFilter(it.colorPrimary)
            binding.ivPaste.setColorFilter(it.colorPrimary)
            binding.ivCamera.setColorFilter(it.colorPrimary)
            binding.ivGallery.setColorFilter(it.colorPrimary)

            binding.progress.progressTintList = ColorStateList.valueOf(it.colorPrimary)

            binding.root.setBackgroundColor(it.colorBackground)
            binding.frameContent.delegate.backgroundColor = it.colorBackground

            binding.frameRootContent.setBackgroundColor(it.colorBackgroundVariant)

            unlockTransition(TAG.THEME.name)
        }

        title.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.tvTitle.text = it

            unlockTransition(TAG.TITLE.name)
        }

        imageInfo.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.ivPicture.setImage(it.image, CircleCrop())
            binding.ivPicture.setVisible(it.isShowImage)

            binding.ivCamera.setVisible(it.isShowInput)
            binding.ivGallery.setVisible(it.isShowInput)

            unlockTransition(TAG.IMAGE.name)
        }

        speakInfo.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.ivStop.setVisible(it.isShowPause)
            binding.ivRead.setVisible(it.isShowPlay)

            unlockTransition(TAG.SPEAK.name)
        }

        clearInfo.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.tvClear.text = it.text
            binding.frameClear.setVisible(it.isShow)
            binding.tvClear.delegate.setBackground(it.background)

            unlockTransition(TAG.CLEAR.name)
        }

        enterInfo.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.etText.hint = it.hint
            binding.etText.setTextColor(it.textColor)

            unlockTransition(TAG.ENTER.name)
        }

        reverseInfo.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.tvReverse.text = it.text
            binding.frameReverse.setVisible(it.isShow)
            binding.tvReverse.delegate.setBackground(it.background)

            unlockTransition(TAG.REVERSE.name)
        }

        listViewItem.asFlow().launchCollect(viewLifecycleOwner) {

            val binding = binding ?: return@launchCollect

            viewModel.awaitTransition()

            binding.recyclerView.submitListAwait(it)

            val transition = TransitionSet().addTransition(ChangeBounds().setDuration(350)).addTransition(Fade().setDuration(350))
            binding.recyclerView.beginTransitionAwait(transition)
        }

        isShowLoading.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.progress.setVisible(it)
        }

        historyViewItemList.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            // hiện rate
            if (binding.etText.text.isBlank() && it.isNotEmpty()) {

                showReview()
            }
        }
    }

    private fun observePhoneticsConfigData() = with(configViewModel) {

        voiceState.observe(viewLifecycleOwner) {

            viewModel.updateSupportSpeak(it.toSuccess()?.data.orEmpty().isNotEmpty())
        }

        listConfig.asFlow().launchCollect(viewLifecycleOwner) {

            val binding = binding ?: return@launchCollect

            viewModel.awaitTransition()

            binding.recFilter.submitListAwait(it)

            val transition = TransitionSet().addTransition(ChangeBounds().setDuration(350)).addTransition(Fade().setDuration(350))
            binding.recFilter.beginTransitionAwait(transition)
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
    }

    private fun startSpeak(text: String) {

        viewModel.startSpeak(
            text = text,

            voiceId = configViewModel.voiceSelect.value ?: 0,
            voiceSpeed = configViewModel.voiceSpeed.value ?: 1f
        )
    }

    private enum class TAG {
        ENTER, IMAGE, CLEAR, TITLE, THEME, SPEAK, REVERSE,
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