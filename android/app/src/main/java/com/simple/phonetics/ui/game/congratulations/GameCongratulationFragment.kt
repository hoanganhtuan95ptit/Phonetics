package com.simple.phonetics.ui.game.congratulations

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.doOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.phonetics.Deeplink
import com.simple.phonetics.EventName
import com.simple.phonetics.Param
import com.simple.phonetics.databinding.DialogCongratulationBinding
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.fragments.BaseSheetFragment
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.phonetics.utils.sendEvent

class CongratulationFragment : BaseSheetFragment<DialogCongratulationBinding, GameCongratulationViewModel>() {

    override fun onDestroy() {
        super.onDestroy()
        sendEvent(EventName.DISMISS, bundleOf())
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

            binding.tvTitle.text = it.title
            binding.tvMessage.text = it.message

            binding.tvAction.text = it.button.text
            binding.tvAction.delegate.setBackground(it.button.background)
        }

        theme.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

            binding.root.delegate.backgroundColor = it.colorBackground
            binding.root.delegate.setBgSelector()

            binding.vAnchor.delegate.backgroundColor = it.colorDivider
            binding.vAnchor.delegate.setBgSelector()
        }

        (arguments?.getLong(Param.NUMBER) ?: 0L).let {

            viewModel.updateNumber(it)
        }
    }
}

@com.tuanha.deeplink.annotation.Deeplink
class GameCongratulationDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return Deeplink.GAME_CONGRATULATION
    }

    override suspend fun navigation(activity: ComponentActivity, deepLink: String, extras: Bundle?, sharedElement: Map<String, View>?): Boolean {

        if (activity !is MainActivity) return false

        val fragment = CongratulationFragment()
        fragment.arguments = extras
        fragment.showOrAwaitDismiss(activity.supportFragmentManager, "")

        return true
    }
}