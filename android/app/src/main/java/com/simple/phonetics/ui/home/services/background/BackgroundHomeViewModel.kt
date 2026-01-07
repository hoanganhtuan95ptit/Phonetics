package com.simple.phonetics.ui.home.services.background

import com.simple.coreapp.ui.view.Size
import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.mutableSharedFlowWithDiff
import com.simple.phonetics.utils.exts.value

class BackgroundHomeViewModel : BaseViewModel() {

    val backgroundSize = mutableSharedFlowWithDiff<Size>()

    val backgroundAlpha = mutableSharedFlowWithDiff<Float>()

    fun updateBackgroundSize(size: Size) {
        if (size == backgroundSize.value) return
        backgroundSize.tryEmit(size)
    }

    fun updateBackgroundAlpha(alpha: Float) {
        if (alpha == backgroundAlpha.value) return
        backgroundAlpha.tryEmit(alpha)
    }
}