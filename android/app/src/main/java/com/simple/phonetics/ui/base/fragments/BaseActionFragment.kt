package com.simple.phonetics.ui.base.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.updatePadding
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.simple.core.utils.extentions.orZero
import com.simple.coreapp.utils.autoCleared
import com.simple.coreapp.utils.ext.DP
import com.simple.coreapp.utils.ext.doOnHeightStatusAndHeightNavigationChange
import com.simple.coreapp.utils.extentions.get
import com.simple.phonetics.utils.exts.listenerHeightChangeAsync
import com.unknown.coroutines.launchCollect
import kotlin.math.absoluteValue

abstract class BaseActionFragment<AVB : ViewBinding, VB : ViewBinding, VM : BaseActionViewModel>() : BaseSheetFragment<VB, VM>() {

    protected abstract fun createBindingAction(): AVB


    protected var bindingAction by autoCleared<AVB>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindingAction = createBindingAction()

        val binding = bindingAction ?: return

        val layoutParam = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        container?.addView(binding.root, layoutParam)


        behavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

            override fun onStateChanged(bottomSheet: View, newState: Int) {

            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

                val translateY = (1 + slideOffset) * this@BaseActionFragment.bottomSheet?.height.orZero() - viewModel.actionHeight.get()
                if (translateY < 0) binding.root.translationY = translateY.absoluteValue
            }
        })

        binding.root.listenerHeightChangeAsync().launchCollect(viewLifecycleOwner) {

            viewModel.updateActionHeight(it)
        }

        doOnHeightStatusAndHeightNavigationChange { heightStatusBar: Int, heightNavigationBar: Int ->

            val bindingAction = bindingAction ?: return@doOnHeightStatusAndHeightNavigationChange

            bindingAction.root.updatePadding(bottom = heightNavigationBar + DP.DP_24)
        }
    }
}