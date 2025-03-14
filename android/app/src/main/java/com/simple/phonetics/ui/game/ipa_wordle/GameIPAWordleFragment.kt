package com.simple.phonetics.ui.game.ipa_wordle

import android.content.ComponentCallbacks
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.asFlow
import com.google.android.flexbox.JustifyContent
import com.simple.adapter.MultiAdapter
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.Param
import com.simple.coreapp.ui.adapters.texts.ClickTextAdapter
import com.simple.coreapp.ui.base.fragments.transition.TransitionFragment
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.doOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.ext.updateMargin
import com.simple.crashlytics.logCrashlytics
import com.simple.phonetics.Deeplink
import com.simple.phonetics.Id
import com.simple.phonetics.R
import com.simple.phonetics.databinding.FragmentListHeaderHorizontalBinding
import com.simple.phonetics.entities.Phonetic
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.base.adapters.ImageStateAdapter
import com.simple.phonetics.ui.game.GameConfigViewModel
import com.simple.phonetics.ui.game.GameFragment
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.phonetics.utils.exts.ListPreviewAdapter
import com.simple.phonetics.utils.exts.collectWithLockTransitionIfCached
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.exts.createFlexboxLayoutManager
import com.simple.phonetics.utils.exts.submitListAwaitV2
import com.simple.phonetics.utils.listenerEvent
import com.simple.phonetics.utils.sendDeeplink
import com.simple.state.isCompleted
import com.simple.state.isRunning
import com.simple.state.isSuccess
import kotlinx.coroutines.delay
import java.util.UUID

class GameIPAWordleFragment : TransitionFragment<FragmentListHeaderHorizontalBinding, GameIPAWordleViewModel>() {

    private val configViewModel: ConfigViewModel by lazy {
        getViewModel(requireActivity(), ConfigViewModel::class)
    }

    private val gameConfigViewModel: GameConfigViewModel by lazy {
        getViewModel(requireActivity(), GameConfigViewModel::class)
    }

    private var adapter by autoCleared<MultiAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {

        val window = activity?.window ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) window.decorView.setOnApplyWindowInsetsListener { view, insets ->

            // Adjust padding to avoid overlap
            view.setPadding(0, 0, 0, 0)

            insets
        } else {
            // For Android 14 and below
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }

        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            WindowCompat.setDecorFitsSystemWindows(window, false)

            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {

            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.root.doOnChangeHeightStatusAndHeightNavigation(viewLifecycleOwner) { heightStatusBar: Int, heightNavigationBar: Int ->

            binding.recyclerView.updatePadding(left = DP.DP_8, right = DP.DP_8)
            binding.frameConfirm.root.updateMargin(bottom = heightNavigationBar + DP.DP_24)
        }

        binding.frameConfirm.root.setDebouncedClickListener {

            viewModel.checkChoose()
        }

        binding.frameHeader.root.setVisible(false)

        setupRecyclerView()

        observeData()
        observeGameConfigData()
    }

    private fun setupRecyclerView() {

        val binding = binding ?: return

        val clickTextAdapter = ClickTextAdapter { view, item ->

            if (item.id.startsWith(Id.CHOOSE)) {
                viewModel.updateChoose(item.data.asObjectOrNull<Phonetic>() ?: return@ClickTextAdapter)
            }
        }

        val imageStateAdapter = ImageStateAdapter { view, item ->

            if (item.id == Id.LISTEN) listen()
        }

        adapter = MultiAdapter(clickTextAdapter, imageStateAdapter, *ListPreviewAdapter()).apply {

            binding.recyclerView.adapter = this
            binding.recyclerView.itemAnimator = null

            val layoutManager = createFlexboxLayoutManager(context = context) {

                logCrashlytics(
                    event = "GAME_IPA_WORDLE",
                    throwable = it,
                    "VIEW_ITEM_SIZE" to "${viewModel.viewItemList.value?.size}"
                )
            }

            layoutManager.justifyContent = JustifyContent.FLEX_START
            binding.recyclerView.layoutManager = layoutManager
        }
    }

    private fun observeData() = with(viewModel) {

        val fragment = this@GameIPAWordleFragment

        val keyRequest = "GAME_IPA_WORDLE_KEY_REQUEST"

        listenerEvent(fragment.viewLifecycleOwner.lifecycle, keyRequest) {

            if (it !is Bundle) return@listenerEvent

            val binding = binding?.frameConfirm ?: return@listenerEvent

            delay(350)

            val transitionName = UUID.randomUUID().toString()
            binding.root.transitionName = transitionName

            if (checkState.value.isSuccess()) sendDeeplink(
                deepLink = Deeplink.GAME_IPA_WORDLE,
                extras = bundleOf(com.simple.phonetics.Param.ROOT_TRANSITION_NAME to transitionName),
                sharedElement = mapOf(transitionName to binding.root)
            ) else {

                viewModel.updateChoose(null)
            }
        }

        stateInfoEvent.asFlow().launchCollect(viewLifecycleOwner) { event ->

            val info = event.getContentIfNotHandled() ?: return@launchCollect

            val extras = bundleOf(
                Param.CANCEL to false,

                Param.TITLE to info.title,
                Param.MESSAGE to info.message,

                Param.BACKGROUND to info.background,

                Param.POSITIVE to info.positive,

                Param.KEY_REQUEST to keyRequest
            )

            sendDeeplink(Deeplink.CONFIRM, extras = extras)
        }


        theme.collectWithLockTransitionUntilData(fragment = fragment, tag = "THEME") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.root.setBackgroundColor(it.colorBackground)
        }

        buttonInfo.collectWithLockTransitionUntilData(fragment = fragment, tag = "BUTTON") {

            val binding = binding?.frameConfirm ?: return@collectWithLockTransitionUntilData

            binding.btnConfirm.text = it.text

            binding.root.isClickable = it.isClickable
            binding.root.delegate.setBackground(it.background)

            binding.root.setVisible(true)
        }

        viewItemList.collectWithLockTransitionIfCached(fragment = fragment, tag = "VIEW_ITEM_LIST") { data, isFirst ->

            val binding = binding ?: return@collectWithLockTransitionIfCached

            binding.recyclerView.submitListAwaitV2(viewItemList = data, isFirst = isFirst)
        }
    }

    private fun observeGameConfigData() = with(gameConfigViewModel) {

        resourceSelected.observe(viewLifecycleOwner) {

            viewModel.updateResource(it)
        }
    }

    private fun listen() {

        val voiceState = viewModel.listenState.value

        if (voiceState.isRunning()) {

            viewModel.stopListen()
        } else if (voiceState == null || voiceState.isCompleted()) viewModel.startListen(
            voiceId = configViewModel.voiceSelect.value ?: 0,
            voiceSpeed = configViewModel.voiceSpeed.value ?: 1f
        )
    }
}

@com.tuanha.deeplink.annotation.Deeplink
class GameIPAWordleDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return Deeplink.GAME_IPA_WORDLE
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Bundle?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is GameFragment) return false

        val fragment = GameIPAWordleFragment()
        fragment.arguments = extras

        val fragmentTransaction = componentCallbacks.childFragmentManager
            .beginTransaction()

        sharedElement?.forEach { (t, u) ->

            fragmentTransaction.addSharedElement(u, t)
        }

        fragmentTransaction
            .replace(R.id.fragment_container, fragment, "")
            .commit()

        return true
    }
}