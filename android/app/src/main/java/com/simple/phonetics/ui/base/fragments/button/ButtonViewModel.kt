package com.simple.phonetics.ui.base.fragments.button

import androidx.lifecycle.LiveData
import com.simple.coreapp.ui.view.Background
import com.simple.coreapp.utils.ext.RichText

interface ButtonViewModel {

    val buttonInfo: LiveData<ButtonInfo>

    data class ButtonInfo(
        val text: RichText,

        val isClickable: Boolean,
        val isShowLoading: Boolean,

        val background: Background,
    )
}