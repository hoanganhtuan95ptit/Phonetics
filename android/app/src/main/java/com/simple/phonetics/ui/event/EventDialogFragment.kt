package com.simple.phonetics.ui.event

import android.content.ComponentCallbacks
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentActivity
import com.simple.core.utils.extentions.orZero
import com.simple.coreapp.Param
import com.simple.coreapp.ui.base.dialogs.sheet.BaseViewBindingSheetFragment
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.ui.view.setBackground
import com.simple.coreapp.utils.ext.ButtonInfo
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.doOnHeightStatusAndHeightNavigationChange
import com.simple.coreapp.utils.ext.getParcelableOrNull
import com.simple.coreapp.utils.ext.setDebouncedClickListener
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.exts.showOrAwaitDismiss
import com.simple.image.setImage
import com.simple.phonetics.Deeplink
import com.simple.phonetics.databinding.DialogEventBinding
import com.simple.phonetics.utils.sendEvent
import com.tuanha.deeplink.DeeplinkHandler

class EventDialogFragment : BaseViewBindingSheetFragment<DialogEventBinding>() {

    private var result: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        doOnHeightStatusAndHeightNavigationChange { heightStatusBar, heightNavigationBar ->

            binding?.background?.updatePadding(bottom = heightNavigationBar + DP.DP_24)
        }

        val binding = binding ?: return

        val isCancelable = arguments?.getBoolean(Param.CANCEL) == true
        dialog?.setCancelable(isCancelable)
        dialog?.setCanceledOnTouchOutside(isCancelable)

        val anim = arguments?.getInt(Param.ANIM).orZero()
        if (anim != 0) binding.ivLogo.setAnimation(anim)

        val image = arguments?.getString(Param.IMAGE).orEmpty()
        if (image.isNotBlank()) binding.ivLogo.setImage(image)

        binding.ivLogo.setVisible(anim != 0 || image.isNotBlank())


        val title = arguments?.getCharSequence(Param.TITLE)
        binding.tvTitle.text = title
        binding.tvTitle.setVisible(!title.isNullOrBlank())

        val message = arguments?.getCharSequence(Param.MESSAGE)
        binding.tvMessage.text = message
        binding.tvMessage.setVisible(!message.isNullOrBlank())

        val anchor = arguments?.getParcelableOrNull<Background>(Param.ANCHOR)
        binding.anchor.delegate.setBackground(anchor)

        val background = arguments?.getParcelableOrNull<Background>(Param.BACKGROUND)
        binding.background.delegate.setBackground(background)


        val negative = arguments?.getParcelableOrNull<ButtonInfo>(Param.NEGATIVE)
        binding.tvNegative.setVisible(negative != null)
        binding.tvNegative.text = negative?.text
        binding.tvNegative.delegate.setBackground(negative?.background)
        binding.tvNegative.setDebouncedClickListener {
            result = 0
            dismiss()
        }

        val positive = arguments?.getParcelableOrNull<ButtonInfo>(Param.POSITIVE)
        binding.tvPositive.setVisible(positive != null)
        binding.tvPositive.text = positive?.text
        binding.tvPositive.delegate.setBackground(positive?.background)
        binding.tvPositive.setDebouncedClickListener {
            result = 1
            dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sendEvent(arguments?.getString(Param.KEY_REQUEST).orEmpty(), result)
    }
}

@com.tuanha.deeplink.annotation.Deeplink
class EventDeeplink : DeeplinkHandler {

    override fun getDeeplink(): String {
        return Deeplink.EVENT
    }

    override suspend fun navigation(activity: ComponentCallbacks, deepLink: String, extras: Map<String, Any?>?, sharedElement: Map<String, View>?): Boolean {

        if (activity !is FragmentActivity) return false

        val fragment = EventDialogFragment()
        fragment.arguments = bundleOf(*extras?.toList().orEmpty().toTypedArray())
        fragment.showOrAwaitDismiss(activity.supportFragmentManager, tag = "")

        return true
    }
}