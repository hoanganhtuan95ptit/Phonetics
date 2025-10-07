package com.simple.phonetics.ui.game.congratulations

import android.content.ComponentCallbacks
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.doOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setText
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.deeplink.DeeplinkHandler
import com.simple.deeplink.annotation.Deeplink
import com.simple.event.sendEvent
import com.simple.phonetics.DeeplinkManager
import com.simple.phonetics.Param
import com.simple.phonetics.databinding.DialogCongratulationBinding
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.fragments.BaseSheetFragment
import com.simple.phonetics.utils.exts.colorDivider
import com.unknown.theme.utils.exts.colorBackground

class CongratulationFragment : BaseSheetFragment<DialogCongratulationBinding, GameCongratulationViewModel>() {

    override fun onDestroy() {
        super.onDestroy()
        sendEvent(arguments?.getString(Param.KEY_REQUEST).orEmpty(), bundleOf())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.root.doOnChangeHeightStatusAndHeightNavigation(viewLifecycleOwner) { heightStatusBar: Int, heightNavigationBar: Int ->

            binding.root.updatePadding(bottom = heightStatusBar + DP.DP_24)
        }

        binding.tvAction.setDebouncedClickListener {

            dismiss()
        }

        observeData()
    }

    private fun observeData() = with(viewModel) {

        info.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.lottieAnimationView.setAnimation(it.anim)
            binding.lottieAnimationView.playAnimation()

            binding.tvTitle.setText(it.title)
            binding.tvMessage.setText(it.message)

            binding.tvAction.setText(it.button.text)
            binding.tvAction.setBackground(it.button.background)
        }

        theme.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.root.setBackground(Background(backgroundColor = it.colorBackground, cornerRadius_TL = DP.DP_16, cornerRadius_TR = DP.DP_16))
            binding.vAnchor.setBackground(Background(backgroundColor = it.colorDivider, cornerRadius = DP.DP_100))
        }

        (arguments?.getLong(Param.NUMBER) ?: 0L).let {

            viewModel.updateNumber(it)
        }
    }
}

@Deeplink
class GameCongratulationDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return DeeplinkManager.GAME_CONGRATULATION
    }

    override suspend fun navigation(activity: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (activity !is MainActivity) return false

        val fragment = CongratulationFragment()
        fragment.arguments = bundleOf(*extras?.toList().orEmpty().toTypedArray())
        fragment.showOrAwaitDismiss(activity.supportFragmentManager, "")

        return true
    }
}