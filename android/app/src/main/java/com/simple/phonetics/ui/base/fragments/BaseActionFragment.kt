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
import com.simple.coreapp.utils.extentions.get
import com.simple.phonetics.utils.exts.listenerHeightChangeAsync
import com.unknown.coroutines.launchCollect
import com.unknown.size.uitls.exts.navigationBarHeight
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.absoluteValue

abstract class BaseActionFragment<AVB : ViewBinding, VB : ViewBinding, VM : BaseActionViewModel>() : BaseSheetFragment<VB, VM>() {

    protected abstract fun createBindingAction(): AVB


    var bindingAction by autoCleared<AVB>()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this@BaseActionFragment.bindingAction = createBindingAction()

        val bindingAction = this@BaseActionFragment.bindingAction ?: return

        val layoutParam = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        container?.addView(bindingAction.root, layoutParam)


        behavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

            override fun onStateChanged(bottomSheet: View, newState: Int) {

            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

                val translateY = (1 + slideOffset) * this@BaseActionFragment.bottomSheet?.height.orZero() - viewModel.actionHeight.get()
                if (translateY < 0) bindingAction.root.translationY = translateY.absoluteValue
                else bindingAction.root.translationY = 0f
            }
        })

        bindingAction.root.listenerHeightChangeAsync().launchCollect(viewLifecycleOwner) {

            viewModel.updateActionHeight(it)
        }

        setupPaddingBottom()
    }

    open fun setupPaddingBottom() {

        viewModel.sizeFlow.map { it.navigationBarHeight }.distinctUntilChanged().launchCollect(viewLifecycleOwner) {

            val bindingAction = bindingAction ?: return@launchCollect

            bindingAction.root.updatePadding(bottom = it + DP.DP_24)
        }
    }
}