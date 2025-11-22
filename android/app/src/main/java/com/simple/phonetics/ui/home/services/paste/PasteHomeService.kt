package com.simple.phonetics.ui.home.services.paste

import android.content.ClipboardManager
import android.content.Context
import com.simple.autobind.annotation.AutoBind
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.clear
import com.simple.coreapp.utils.extentions.haveText
import com.simple.coreapp.utils.extentions.text
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import com.simple.phonetics.ui.home.services.HomeService
import com.unknown.coroutines.launchCollect
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import org.koin.androidx.viewmodel.ext.android.viewModel

@AutoBind(HomeFragment::class)
class PasteHomeService : HomeService {

    override fun setup(homeFragment: HomeFragment) {

        val homeViewModel: HomeViewModel by homeFragment.viewModel()

        val clipboard = homeFragment.requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        clipboard.listenerPrimaryClipChangedAsync().launchCollect(homeFragment.viewLifecycleOwner) {

            val binding = homeFragment.binding ?: return@launchCollect

            binding.ivPaste.setVisible(it)
        }


        val binding = homeFragment.binding ?: return

        binding.ivPaste.post {

            binding.ivPaste.setVisible(clipboard.haveText())
        }

        binding.ivPaste.setOnClickListener {

            homeViewModel.getPhonetics("")
            binding.etText.setText(clipboard.text() ?: "")

            clipboard.clear()
        }
    }

    private fun ClipboardManager.listenerPrimaryClipChangedAsync() = channelFlow<Boolean> {

        val onPrimaryClipChangedListener = ClipboardManager.OnPrimaryClipChangedListener {

            trySend(haveText())
        }

        addPrimaryClipChangedListener(onPrimaryClipChangedListener)

        awaitClose {
            removePrimaryClipChangedListener(onPrimaryClipChangedListener)
        }
    }
}