package com.simple.phonetics.ui.game.congratulations

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import com.simple.core.utils.extentions.orZero
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.phonetics.Deeplink
import com.simple.phonetics.EventName
import com.simple.phonetics.Param
import com.simple.phonetics.databinding.DialogCongratulationBinding
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.ui.base.fragments.BaseSheetFragment
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.phonetics.utils.sendDeeplink

class CongratulationFragment : BaseSheetFragment<DialogCongratulationBinding, GameCongratulationViewModel>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeData()
    }

    private fun observeData() = with(viewModel) {

        info.observe(viewLifecycleOwner) {

            val binding = binding ?: return@observe

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

        arguments?.getInt(Param.NUMBER).orZero().let {

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

        sendDeeplink(EventName.DISMISS)

        return true
    }
}