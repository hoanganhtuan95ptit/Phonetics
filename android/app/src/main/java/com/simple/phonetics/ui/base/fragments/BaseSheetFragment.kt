package com.simple.phonetics.ui.base.fragments

import android.os.Bundle
import android.view.View
import androidx.viewbinding.ViewBinding
import com.simple.coreapp.ui.base.dialogs.sheet.BaseViewModelSheetFragment

abstract class BaseSheetFragment<T : ViewBinding, VM : BaseViewModel>() : BaseViewModelSheetFragment<T, VM>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}