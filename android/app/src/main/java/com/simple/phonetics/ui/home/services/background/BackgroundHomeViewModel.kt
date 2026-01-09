package com.simple.phonetics.ui.home.services.background

import com.simple.phonetics.ui.base.fragments.BaseViewModel
import com.simple.phonetics.utils.exts.mutableSharedFlowWithDiff
import com.simple.phonetics.utils.exts.value

class BackgroundHomeViewModel : BaseViewModel() {

    val backgroundAlpha = mutableSharedFlowWithDiff<Float>()

    fun updateBackgroundAlpha(alpha: Float) {
        if (alpha == backgroundAlpha.value) return
        backgroundAlpha.tryEmit(alpha)
    }
}