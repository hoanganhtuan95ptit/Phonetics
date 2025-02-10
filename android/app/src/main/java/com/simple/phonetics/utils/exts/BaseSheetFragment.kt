package com.simple.phonetics.utils.exts

import androidx.fragment.app.FragmentManager
import com.simple.coreapp.ui.base.dialogs.OnDismissListener
import com.simple.coreapp.ui.base.dialogs.sheet.BaseSheetFragment
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull

suspend fun BaseSheetFragment.showAwaitDismiss(fragmentManager: FragmentManager, tag: String = "") = channelFlow {

    show(fragmentManager, tag)

    onDismissListener = object : OnDismissListener {

        override fun onDismiss() {

            trySend(Unit)
        }
    }

    awaitClose {
        dismiss()
    }
}.firstOrNull()