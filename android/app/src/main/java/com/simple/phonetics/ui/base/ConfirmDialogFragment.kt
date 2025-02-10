package com.simple.phonetics.ui.base

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.os.bundleOf
import com.simple.core.utils.extentions.orZero
import com.simple.coreapp.Param
import com.simple.coreapp.ui.base.dialogs.sheet.BaseViewBindingSheetFragment
import com.simple.coreapp.utils.ext.doOnHeightStatusAndHeightNavigationChange
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.phonetics.Deeplink
import com.simple.phonetics.databinding.DialogConfirmVerticalV2Binding
import com.simple.phonetics.ui.MainActivity
import com.simple.phonetics.utils.DeeplinkHandler
import com.simple.phonetics.utils.exts.showAwaitDismiss
import com.simple.phonetics.utils.sendEvent

class ConfirmDialogFragment : BaseViewBindingSheetFragment<DialogConfirmVerticalV2Binding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        doOnHeightStatusAndHeightNavigationChange { _, heightNavigationBar ->

            binding?.root?.setPadding(0, 0, 0, heightNavigationBar)
        }

        val binding = binding ?: return

        val isCancelable = arguments?.getBoolean(Param.PARAM_CANCEL) == true
        dialog?.setCancelable(isCancelable)
        dialog?.setCanceledOnTouchOutside(isCancelable)

        val title = arguments?.getString(Param.PARAM_TITLE)
        binding.tvTitle.text = title
        binding.tvTitle.setVisible(!title.isNullOrBlank())

        val image = arguments?.getInt(Param.PARAM_IMAGE).orZero()
        if (image != 0) binding.ivLogo.setAnimation(image)
        binding.ivLogo.setVisible(image != 0)

        val message = arguments?.getString(Param.PARAM_MESSAGE)
        binding.tvMessage.text = message
        binding.tvMessage.setVisible(!message.isNullOrBlank())

        val negative = arguments?.getString(Param.PARAM_NEGATIVE)
        binding.tvNegative.setVisible(!negative.isNullOrBlank())
        binding.tvNegative.text = negative
        binding.tvNegative.setDebouncedClickListener {
            sendEvent(arguments?.getString(Param.KEY_REQUEST).orEmpty(), bundleOf(Param.RESULT_CODE to 0))
            dismiss()
        }

        val positive = arguments?.getString(Param.PARAM_POSITIVE)
        binding.tvPositive.setVisible(!positive.isNullOrBlank())
        binding.tvPositive.text = positive
        binding.tvPositive.setDebouncedClickListener {
            sendEvent(arguments?.getString(Param.KEY_REQUEST).orEmpty(), bundleOf(Param.RESULT_CODE to 1))
            dismiss()
        }
    }
}

@com.tuanha.deeplink.annotation.Deeplink
class ConfirmDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return Deeplink.CONFIRM
    }

    override suspend fun navigation(activity: ComponentActivity, deepLink: String, extras: Bundle?, sharedElement: Map<String, View>?): Boolean {

        if (activity !is MainActivity) return false

        val fragment = ConfirmDialogFragment()
        fragment.arguments = extras
        fragment.showAwaitDismiss(activity.supportFragmentManager)

        return true
    }
}