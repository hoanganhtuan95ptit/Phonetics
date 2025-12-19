package com.simple.phonetics.ui.game

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.simple.analytics.logAnalytics
import com.simple.coreapp.utils.ext.doOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.getStringOrEmpty
import com.simple.coreapp.utils.ext.getViewModel
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.deeplink.sendDeeplink
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.databinding.FragmentContainerHeaderHorizontalBinding
import com.simple.phonetics.ui.base.fragments.BaseFragment
import com.simple.phonetics.utils.exts.collectWithLockTransitionUntilData
import com.simple.phonetics.utils.exts.replace
import com.unknown.theme.utils.exts.colorBackground
import kotlinx.coroutines.launch

class GameFragment : BaseFragment<FragmentContainerHeaderHorizontalBinding, GameViewModel>() {

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

        binding.root.doOnChangeHeightStatusAndHeightNavigation(viewLifecycleOwner) { heightStatusBar: Int, _: Int ->

            binding.root.updatePadding(top = heightStatusBar)
        }

        binding.frameHeader.icBack.setDebouncedClickListener {

            activity?.supportFragmentManager?.popBackStack()
        }

        observeData()
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


        val resource = arguments.getStringOrEmpty(Param.RESOURCE)

        if (resource.isNotEmpty()) {

            viewModel.updateResourceSelected(resource)
        } else gameConfigViewModel.resourceSelected.observe(viewLifecycleOwner) {

            viewModel.updateResourceSelected(it)
        }


        viewLifecycleOwner.lifecycleScope.launch {

            sendDeeplink(gameConfigViewModel.getNextGame(), extras = mapOf(Param.FIRST to true))
        }
    }
}

@Deeplink
class GameDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return DeeplinkManager.GAME
    }

    override suspend fun navigation(activity: AppCompatActivity, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        activity.supportFragmentManager.replace(fragment = GameFragment(), extras = extras, sharedElement = sharedElement)

        return true
    }
}