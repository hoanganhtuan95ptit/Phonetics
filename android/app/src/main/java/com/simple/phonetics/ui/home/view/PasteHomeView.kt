package com.simple.phonetics.ui.home.view

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.clear
import com.simple.coreapp.utils.extentions.haveText
import com.simple.coreapp.utils.extentions.text
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.HomeViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

interface PasteHomeView {

    fun setupPaste(fragment: HomeFragment)
}

class PasteHomeViewImpl : PasteHomeView {

    override fun setupPaste(fragment: HomeFragment) {

        val viewModel: HomeViewModel by fragment.viewModel()

        val clipboard = fragment.requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val onPrimaryClipChangedListener = ClipboardManager.OnPrimaryClipChangedListener {

            val binding = fragment.binding ?: return@OnPrimaryClipChangedListener

            binding.ivPaste.setVisible(clipboard.haveText())
        }

        clipboard.addPrimaryClipChangedListener(onPrimaryClipChangedListener)

        fragment.lifecycle.addObserver(object : LifecycleEventObserver {

            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_DESTROY -> clipboard.removePrimaryClipChangedListener(onPrimaryClipChangedListener)
                    else -> Unit
                }
            }
        })


        val binding = fragment.binding ?: return

        binding.ivPaste.post {

            binding.ivPaste.setVisible(clipboard.haveText())
        }

        binding.ivPaste.setOnClickListener {

            viewModel.getPhonetics("")
            binding.etText.setText(clipboard.text() ?: "")

            clipboard.clear()
        }
    }
}