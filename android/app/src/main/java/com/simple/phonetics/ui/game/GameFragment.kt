package com.simple.phonetics.ui.game

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.simple.analytics.logAnalytics
import com.simple.coreapp.utils.ext.doOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.phonetics.Deeplink
import com.simple.phonetics.Param
import com.simple.phonetics.R
import com.simple.phonetics.databinding.FragmentContainerHeaderHorizontalBinding
import com.simple.phonetics.ui.ConfigViewModel
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.fragments.BaseFragment
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.phonetics.utils.DeeplinkView
import com.simple.phonetics.utils.DeeplinkViewImpl
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.sendDeeplink
import kotlinx.coroutines.launch

class GameFragment : BaseFragment<FragmentContainerHeaderHorizontalBinding, GameViewModel>(),
    DeeplinkView by DeeplinkViewImpl() {

    private val configViewModel: ConfigViewModel by lazy {
        getViewModel(requireActivity(), ConfigViewModel::class)
    }

    private val gameConfigViewModel: GameConfigViewModel by lazy {
        getViewModel(requireActivity(), GameConfigViewModel::class)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logAnalytics("game_show")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.root.doOnChangeHeightStatusAndHeightNavigation(viewLifecycleOwner) { heightStatusBar: Int, heightNavigationBar: Int ->

            binding.root.updatePadding(top = heightStatusBar)
        }

        binding.frameHeader.icBack.setDebouncedClickListener {

            activity?.supportFragmentManager?.popBackStack()
        }

        setupDeeplink(this)

        observeData()
        observeConfigData()
    }

    private fun observeData() = with(viewModel) {

        val fragment = this@GameFragment

        title.collectWithLockTransitionUntilData(fragment = fragment, tag = "TITLE") {

            val binding = binding?.frameHeader ?: return@collectWithLockTransitionUntilData

            binding.tvTitle.text = it
        }

        theme.collectWithLockTransitionUntilData(fragment = fragment, tag = "THEME") {

            val binding = binding ?: return@collectWithLockTransitionUntilData

            binding.root.setBackgroundColor(it.colorBackground)
        }

        viewLifecycleOwner.lifecycleScope.launch {

            sendDeeplink(gameConfigViewModel.getNextGame(), extras = bundleOf(Param.FIRST to true))
        }
    }

    private fun observeConfigData() = with(configViewModel) {

        phoneticSelect.observe(viewLifecycleOwner) {

            viewModel.updatePhoneticCodeSelected(it)
        }
    }
}

@com.tuanha.deeplink.annotation.Deeplink
class GameDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return Deeplink.GAME
    }

    override suspend fun navigation(activity: ComponentActivity, deepLink: String, extras: Bundle?, sharedElement: Map<String, View>?): Boolean {

        if (activity !is MainActivity) return false

        val fragment = GameFragment()
        fragment.arguments = extras

        val fragmentTransaction = activity.supportFragmentManager
            .beginTransaction()

        sharedElement?.forEach { (t, u) ->

            fragmentTransaction.addSharedElement(u, t)
        }

        fragmentTransaction
            .replace(R.id.fragment_container, fragment, "")
            .addToBackStack("")
            .commit()

        return true
    }
}