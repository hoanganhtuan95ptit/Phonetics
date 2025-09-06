package com.simple.phonetics.ui.base.fragments.header

import androidx.lifecycle.LiveData
import com.simple.coreapp.utils.ext.RichText

interface HeaderViewModel {

    val headerInfo: LiveData<HeaderInfo>

    data class HeaderInfo(
        val title: RichText,
        val message: RichText,
    )
}