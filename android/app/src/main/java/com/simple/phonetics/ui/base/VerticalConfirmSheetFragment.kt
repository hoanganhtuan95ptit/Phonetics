package com.simple.phonetics.ui.base

import android.content.ComponentCallbacks
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResult
import com.simple.adapter.entities.ViewItem
import com.simple.coreapp.Param
import com.simple.coreapp.ui.base.dialogs.sheet.BaseViewBindingSheetFragment
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.ButtonInfo
import com.simple.coreapp.utils.ext.doOnHeightStatusAndHeightNavigationChange
import com.simple.coreapp.utils.ext.getParcelableOrNull
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.submitListAwait
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.phonetics.databinding.DialogListBinding
import com.simple.phonetics.databinding.LayoutActionVerticalBinding
import com.tuanha.deeplink.DeeplinkHandler
import com.tuanha.deeplink.annotation.Deeplink
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

private val viewItemList by lazy {

    MutableSharedFlow<List<ViewItem>>(replay = 1, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.SUSPEND)
}

class VerticalConfirmSheetFragment : BaseViewBindingSheetFragment<DialogListBinding>() {

    private var bindingConfirmSpeak by autoCleared<LayoutActionVerticalBinding>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindingConfirmSpeak = LayoutActionVerticalBinding.inflate(LayoutInflater.from(requireContext()))

        val layoutParam = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        container?.addView(bindingConfirmSpeak?.root, layoutParam)

        doOnHeightStatusAndHeightNavigationChange { heightStatusBar, heightNavigationBar ->

            binding?.root?.setPadding(0, 0, 0, heightNavigationBar)
        }

        val isCancelable = arguments?.getBoolean(Param.CANCEL) == true
        dialog?.setCancelable(isCancelable)
        dialog?.setCanceledOnTouchOutside(isCancelable)


        val binding = binding ?: return

        viewItemList.launchCollect(viewLifecycleOwner) {

            binding.recyclerView.submitListAwait(it)
        }


        val layoutActionVerticalBinding = bindingConfirmSpeak ?: return

        val negative = arguments?.getParcelableOrNull<ButtonInfo>(Param.NEGATIVE)
        layoutActionVerticalBinding.tvNegative.setVisible(negative != null)
        layoutActionVerticalBinding.tvNegative.text = negative?.text
        layoutActionVerticalBinding.tvNegative.delegate.setBackground(negative?.background)
        layoutActionVerticalBinding.tvNegative.setDebouncedClickListener {
            setFragmentResult(arguments?.getString(Param.KEY_REQUEST).orEmpty(), bundleOf(Param.RESULT_CODE to 0))
            dismiss()
        }

        val positive = arguments?.getParcelableOrNull<ButtonInfo>(Param.POSITIVE)
        layoutActionVerticalBinding.tvPositive.setVisible(positive != null)
        layoutActionVerticalBinding.tvPositive.text = positive?.text
        layoutActionVerticalBinding.tvPositive.delegate.setBackground(positive?.background)
        layoutActionVerticalBinding.tvPositive.setDebouncedClickListener {
            setFragmentResult(arguments?.getString(Param.KEY_REQUEST).orEmpty(), bundleOf(Param.RESULT_CODE to 1))
            dismiss()
        }
    }
}

@Deeplink(queue = "Confirm")
class ConfirmDeeplinkHandler : DeeplinkHandler {

    override fun getDeeplink(): String {
        return com.simple.phonetics.Deeplink.CONFIRM
    }

    override suspend fun navigation(componentCallbacks: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (componentCallbacks !is FragmentActivity) {
            return false
        }

        val fragment = VerticalConfirmSheetFragment()

        viewItemList.resetReplayCache()
        viewItemList.emit(extras?.values?.filterIsInstance<ViewItem>().orEmpty())

        fragment.arguments = extras?.filter {

            it !is ViewItem
        }?.let {

            bundleOf(*it.toList().toTypedArray())
        }

        fragment.showOrAwaitDismiss(componentCallbacks.supportFragmentManager, "")

        return true
    }
}